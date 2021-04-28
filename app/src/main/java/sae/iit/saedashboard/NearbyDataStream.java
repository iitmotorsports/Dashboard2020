package sae.iit.saedashboard;

import android.app.Activity;
import android.app.AlertDialog;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

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

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

public class NearbyDataStream {
    private static final String BROADCASTER_USERNAME = "IIT SAE Broadcaster";
    private static final String RECEIVER_USERNAME = "IIT SAE Receiver";
    private static final String SERVICE_ID = "RAW_DATA_STREAM";
    private final LinkedBlockingQueue<byte[]> buffer = new LinkedBlockingQueue<>();
    private final Activity activity;
    private boolean broadcast = false;
    private boolean sendData = false;
    private boolean connected = false;
    private AlertDialog acceptDialog;
    private TextView authText, connName;
    private String currentEndpointId, pendingEndpointId = "";
    private DataReceiver dr;
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

    private void createAcceptDialog() {
        AlertDialog.Builder mBuilder = new AlertDialog.Builder(activity);
        View mView = activity.getLayoutInflater().inflate(R.layout.nearby_accept_layout, null);

        Button acceptBtn = mView.findViewById(R.id.acceptBtn);
        Button rejectBtn = mView.findViewById(R.id.rejectBtn);
        authText = mView.findViewById(R.id.authText);
        connName = mView.findViewById(R.id.connName);

        mBuilder.setView(mView);
        acceptDialog = mBuilder.create();

        AtomicBoolean accepted = new AtomicBoolean(false);

        acceptBtn.setOnClickListener(v -> {
            accepted.set(true);
            acceptConnection(pendingEndpointId);
            acceptDialog.dismiss();
        });

        rejectBtn.setOnClickListener(v -> {
            rejectConnection(pendingEndpointId);
            acceptDialog.dismiss();
        });

        acceptDialog.setOnDismissListener(dialog -> {
            if (!accepted.get())
                rejectConnection(pendingEndpointId);
            accepted.set(false);
        });

    }

    ConnectionLifecycleCallback connectionLifecycleCallback = new ConnectionLifecycleCallback() {
        @Override
        public void onConnectionInitiated(@NonNull String endpointId, @NonNull ConnectionInfo info) {
            pendingEndpointId = endpointId;
            String authTok = info.getAuthenticationToken();
            String endName = info.getEndpointName();
            activity.runOnUiThread(() -> {
                authText.setText(authTok);
                connName.setText(endName);
                acceptDialog.show();
            });
        }

        @Override
        public void onConnectionResult(@NonNull String endpointId, @NonNull ConnectionResolution result) {
            switch (result.getStatus().getStatusCode()) {
                case ConnectionsStatusCodes.STATUS_OK:
                    Toaster.showToast("Data stream connected", Toaster.STATUS.SUCCESS);
                    connected = true;
                    client.stopAdvertising();
                    client.stopDiscovery();
                    if (broadcast)
                        sendData = true;
                    return;
                case ConnectionsStatusCodes.STATUS_CONNECTION_REJECTED:
                    Toaster.showToast("Data stream connection rejected", Toaster.STATUS.ERROR);
                    acceptDialog.dismiss();
                    break;
                case ConnectionsStatusCodes.STATUS_ERROR:
                    Toaster.showToast("Data stream connection dropped", Toaster.STATUS.ERROR);
                    break;
                default:
                    Toaster.showToast("Data stream unknown error", Toaster.STATUS.ERROR);
            }
            connected = false;
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
            client.requestConnection(broadcast ? BROADCASTER_USERNAME : RECEIVER_USERNAME, endpointId, connectionLifecycleCallback);
        }

        @Override
        public void onEndpointLost(@NonNull String s) {
            Toaster.showToast("Lost Endpoint", Toaster.STATUS.INFO);
        }
    };

    private void startAdvertising() {
        client.startAdvertising(broadcast ? BROADCASTER_USERNAME : RECEIVER_USERNAME, SERVICE_ID, connectionLifecycleCallback, advertisingOptions)
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
        createAcceptDialog();
    }

    public void setReceiver(DataReceiver dataReceiver) {
        dr = dataReceiver;
    }

    public void sendPayload(byte[] data) {
        if (sendData) {
            client.sendPayload(currentEndpointId, Payload.fromBytes(data));
        }
    }

    public void enableBroadcast() {
        if (connected) {
            broadcast = true;
            sendData = true;
        }
    }

    public void broadcast() {
        broadcast = true;
        startAdvertising();
        startDiscovery();
    }

    public boolean isConnected() {
        return connected;
    }

    public void receive() {
        if (dr == null) {
            Toaster.showToast("No Receiver set", Toaster.STATUS.ERROR);
            return;
        }
        broadcast = false;
        startAdvertising();
        startDiscovery();
    }

    public void stop() {
        broadcast = false;
        sendData = false;
        connected = false;
        client.stopAllEndpoints();
        client.stopAdvertising();
        client.stopDiscovery();
    }
}
