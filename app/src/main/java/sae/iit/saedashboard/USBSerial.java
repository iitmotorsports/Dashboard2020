package sae.iit.saedashboard;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.util.Log;

import com.felhr.usbserial.UsbSerialDevice;
import com.felhr.usbserial.UsbSerialInterface;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class USBSerial {

    private static final int TEENSY_VID = 5824;
    public final static String ACTION_USB_PERMISSION = "com.hariharan.arduinousb.USB_PERMISSION";
    private final static String LOG_ID = "USBSerial";
    private final BroadcastReceiver broadcastReceiver;
    private UsbDeviceConnection connection;
    private final UsbManager usbManager;
    private UsbSerialDevice serialPort;
    private final Activity activity;
    private UsbDevice device;
    private boolean connected = false;

    public interface DeviceActionCallback {
        void callback();
    }

    public USBSerial(Activity activity, UsbSerialInterface.UsbReadCallback mCallback, DeviceActionCallback attach, DeviceActionCallback detach) {
        this.activity = activity;

        usbManager = (UsbManager) activity.getSystemService(Context.USB_SERVICE);
        broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                try {
                    switch (Objects.requireNonNull(intent.getAction())) {
                        case ACTION_USB_PERMISSION:
                            if (Objects.requireNonNull(intent.getExtras()).getBoolean(UsbManager.EXTRA_PERMISSION_GRANTED)) {
                                connection = usbManager.openDevice(device);
                                serialPort = UsbSerialDevice.createUsbSerialDevice(device, connection);
                                if (serialPort != null && serialPort.open()) {
                                    serialPort.setBaudRate(115200);
                                    serialPort.setDataBits(UsbSerialInterface.DATA_BITS_8);
                                    serialPort.setStopBits(UsbSerialInterface.STOP_BITS_2);
                                    serialPort.setParity(UsbSerialInterface.PARITY_NONE);
                                    serialPort.setFlowControl(UsbSerialInterface.FLOW_CONTROL_OFF);
//                                    setUIButtonsOn(true);
                                    serialPort.read(mCallback);
                                    Log.i(LOG_ID, "Serial Connection Opened!");
                                    connected = true;
                                    attach.callback();
                                }
                            }
                            break;
                        case UsbManager.ACTION_USB_DEVICE_ATTACHED:
                            open();
                            break;
                        case UsbManager.ACTION_USB_DEVICE_DETACHED:
                            connected = false;
                            detach.callback();
                            break;
                    }
                } catch (NullPointerException ignored) {
                }
            }
        };

        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_USB_PERMISSION);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        activity.registerReceiver(broadcastReceiver, filter);
        Log.i(LOG_ID, "Receiver registered");
    }

    public boolean open() {
        HashMap<String, UsbDevice> usbDevices = usbManager.getDeviceList();
        if (!usbDevices.isEmpty()) {
            for (Map.Entry<String, UsbDevice> entry : usbDevices.entrySet()) {
                device = entry.getValue();
                int deviceVID = device.getVendorId();
                if (deviceVID == TEENSY_VID) {
                    PendingIntent pi = PendingIntent.getBroadcast(activity, 0, new Intent(ACTION_USB_PERMISSION), 0);
                    usbManager.requestPermission(device, pi);
                    return true;
                } else {
                    Log.i(LOG_ID, String.valueOf(deviceVID));
                    connection = null;
                    device = null;
                }
            }
        }
        return false;
    }

    public void close() {
        if (connected) {
            connected = false;
            serialPort.close();
            Log.i(LOG_ID, "Serial Connection Closed!");
        }
    }

    @Override
    protected void finalize() throws Throwable {
        activity.unregisterReceiver(broadcastReceiver);
        Log.i(LOG_ID, "Receiver unregistered");
        super.finalize();
    }

    public void write(byte[] buffer) {
        if (connected)
            serialPort.write(buffer);
    }

    public boolean isConnected() {
        return connected;
    }

}
