package sae.iit.saedashboard;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.nearby.Nearby;
import com.google.android.gms.nearby.connection.AdvertisingOptions;
import com.google.android.gms.nearby.connection.ConnectionInfo;
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback;
import com.google.android.gms.nearby.connection.ConnectionResolution;
import com.google.android.gms.nearby.connection.ConnectionsClient;
import com.google.android.gms.nearby.connection.ConnectionsStatusCodes;
import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo;
import com.google.android.gms.nearby.connection.DiscoveryOptions;
import com.google.android.gms.nearby.connection.EndpointDiscoveryCallback;
import com.google.android.gms.nearby.connection.Payload;
import com.google.android.gms.nearby.connection.PayloadCallback;
import com.google.android.gms.nearby.connection.PayloadTransferUpdate;
import com.google.android.gms.nearby.connection.Strategy;

import java.util.Arrays;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;

public class NearbyDataStream {
    private static final String USERNAME = "IIT_SAE";
    private static final String SERVICE_ID = "RAW_DATA_STREAM";
    private static final String ENDPOINT_ID = "SAE RAW DATA STREAM";
    private final LinkedBlockingQueue<byte[]> buffer = new LinkedBlockingQueue<>();
    private final Activity activity;
    private boolean broadcast = false;
    private boolean sendData = false;
    private String currentEndpointId = "";
    private DataReceiver dr;
    private final Streamer stream;
    private final ConnectionsClient client;
    private final AdvertisingOptions advertisingOptions = new AdvertisingOptions.Builder().setStrategy(Strategy.P2P_POINT_TO_POINT).build();
    private final DiscoveryOptions discoveryOptions = new DiscoveryOptions.Builder().setStrategy(Strategy.P2P_POINT_TO_POINT).build();

    public interface DataReceiver {
        void run(byte[] rawData);
    }

    PayloadCallback payloadCallback = new PayloadCallback() {
        @Override
        public void onPayloadReceived(@NonNull String s, @NonNull Payload payload) {
            dr.run(payload.asBytes());
        }

        @Override
        public void onPayloadTransferUpdate(@NonNull String s, @NonNull PayloadTransferUpdate payloadTransferUpdate) {
        }
    };

    private void acceptConnection(String endpointId) {
        Toaster.showToast("Connection accepted with " + endpointId, Toaster.STATUS.SUCCESS);
        currentEndpointId = endpointId;
        client.acceptConnection(endpointId, payloadCallback);
    }

    private void rejectConnection(String endpointId) {
        Toaster.showToast("Connection Rejected!", Toaster.STATUS.ERROR);
        client.rejectConnection(endpointId);
    }

    ConnectionLifecycleCallback connectionLifecycleCallback = new ConnectionLifecycleCallback() {
        @Override
        public void onConnectionInitiated(@NonNull String endpointId, @NonNull ConnectionInfo info) {
//            acceptConnection(endpointId);
            Toaster.showToast("Accept Connection", Toaster.STATUS.INFO);
            AlertDialog builder = new AlertDialog.Builder(activity).setIcon(android.R.drawable.ic_dialog_alert)
                    .setPositiveButton("Accept", (DialogInterface dialog, int which) -> acceptConnection(endpointId))
                    .setNegativeButton(android.R.string.cancel, (DialogInterface dialog, int which) -> rejectConnection(endpointId))
                    .setTitle("Accept connection to " + info.getEndpointName())
                    .setMessage("Confirm the code matches on both devices: " + info.getAuthenticationToken()).create();
            builder.dismiss();
            builder.show();
        }

        @Override
        public void onConnectionResult(@NonNull String endpointId, @NonNull ConnectionResolution result) {
            switch (result.getStatus().getStatusCode()) {
                case ConnectionsStatusCodes.STATUS_OK:
                    Toaster.showToast("Data stream connected", Toaster.STATUS.SUCCESS);
                    client.stopAdvertising();
                    client.stopDiscovery();
                    if (broadcast)
                        sendData = true;
//                        stream.execute(null);
                    break;
                case ConnectionsStatusCodes.STATUS_CONNECTION_REJECTED:
                    Toaster.showToast("Data stream connection rejected", Toaster.STATUS.ERROR);
                    break;
                case ConnectionsStatusCodes.STATUS_ERROR:
                    Toaster.showToast("Data stream connection broke", Toaster.STATUS.ERROR);
                    break;
                default:
                    Toaster.showToast("Data stream unknown error", Toaster.STATUS.ERROR);
            }

        }

        @Override
        public void onDisconnected(@NonNull String s) {
            sendData = false;
            Toaster.showToast("Data stream disconnected", Toaster.STATUS.WARNING);
        }
    };

    EndpointDiscoveryCallback endpointDiscoveryCallback = new EndpointDiscoveryCallback() {
        @Override
        public void onEndpointFound(@NonNull String endpointId, @NonNull DiscoveredEndpointInfo discoveredEndpointInfo) {
            Toaster.showToast("Found Endpoint", Toaster.STATUS.INFO);
            client.requestConnection(broadcast ? "Broadcaster" : "Receiver", endpointId, connectionLifecycleCallback);
        }

        @Override
        public void onEndpointLost(@NonNull String s) {
            Toaster.showToast("Lost Endpoint", Toaster.STATUS.INFO);
        }
    };

    private class Streamer implements Executor {
        @Override
        public void execute(Runnable command) {
            while (broadcast) {
                byte[] newData = buffer.poll();
                if (newData != null)
                    client.sendPayload(currentEndpointId, Payload.fromBytes(newData));
            }
        }
    }

    private void startAdvertising() {
        client.startAdvertising(USERNAME, SERVICE_ID, connectionLifecycleCallback, advertisingOptions)
                .addOnSuccessListener((Void unused) -> Toaster.showToast("Searching for receiver", Toaster.STATUS.INFO))
                .addOnFailureListener((Exception e) -> Toaster.showToast("Failed to start search for a receiver", true, Toaster.STATUS.ERROR));
    }

    private void startDiscovery() {
        client.startDiscovery(SERVICE_ID, endpointDiscoveryCallback, discoveryOptions)
                .addOnSuccessListener((Void unused) -> Toaster.showToast("Searching for broadcaster", Toaster.STATUS.INFO))
                .addOnFailureListener((Exception e) -> Toaster.showToast("Failed to start search for a broadcaster", true, Toaster.STATUS.ERROR));
    }


    public NearbyDataStream(Activity activity) {
        client = Nearby.getConnectionsClient(activity.getApplicationContext());
        this.activity = activity;
        stream = new Streamer();
    }

    public void setReceiver(DataReceiver dataReceiver) {
        dr = dataReceiver;
    }

    public void queue(byte[] data) {
//        buffer.add(data);
        if (sendData) {
//            Log.i("Stream", currentEndpointId + Arrays.toString(data));
            client.sendPayload(currentEndpointId, Payload.fromBytes(data));
        }
    }

    public void broadcast() {
        broadcast = true;
        startAdvertising();
        startDiscovery();
    }

    public void receive() {
        if (dr == null) {
            Toaster.showToast("No Receiver set", Toaster.STATUS.ERROR);
            return;
        }
        startAdvertising();
        startDiscovery();
    }

    public void stop() {
        broadcast = false;
        sendData = false;
        client.stopAllEndpoints();
//        client.disconnectFromEndpoint(ENDPOINT_ID);
        client.stopAdvertising();
        client.stopDiscovery();
    }
}
