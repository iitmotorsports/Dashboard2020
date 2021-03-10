package sae.iit.saedashboard;

import android.app.Activity;
import android.content.Intent;
import android.util.Log;

import com.felhr.usbserial.UsbSerialInterface;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public class TeensyStream {
    private final HashMap<Long, msgBlock> Teensy_Data = new HashMap<>();
    private final String FILENAME_SAVE = "TEENSY_JSON_MAP.json";
    private final TeensyLogBooleanCallback callOnLoad;
    private final String LOG_TAG = "Teensy Stream";
    private final USBSerial serialConnection;
    private final JSONLoad loader;

    private HashMap<Integer, String> Teensy_LookUp_Tag = new HashMap<>();
    private HashMap<Integer, String> Teensy_LookUp_Str = new HashMap<>();
    private FileOutputStream logFile;

    private boolean enableLogCallback = true;
    private boolean enableLogFile;
    private boolean JSONLoaded = false;
    private boolean hexMode = false;

    private final HashMap<Long, STATE> Teensy_State_Map = new HashMap<>();
    private long currentState = 0;

    // region Interfaces

    /**
     * Simple callback class, used in multiple functions
     */
    public interface TeensyCallback extends USBSerial.DeviceActionCallback {
    }

    /**
     * Callback class which accepts a long, typically the value of a message
     */
    public interface TeensyLongCallback {
        void callback(long num);
    }

    /**
     * Callback class which accepts a string, typically the string interpretation of a value
     */
    public interface TeensyLogCallback {
        void callback(String msg);
    }

    /**
     * Callback class which accepts a boolean, typically whether a JSON map was successfully loaded
     */
    public interface TeensyLogBooleanCallback {
        void callback(boolean jsonLoaded);
    }

    // endregion

    // region Getter/Setters

    /**
     * @return Whether the logging callback is being called at all
     */
    public boolean isEnableLogCallback() {
        return enableLogCallback;
    }

    /**
     * @param enableLogCallback Set whether the logging callback should be called at all
     */
    public void setEnableLogCallback(boolean enableLogCallback) {
        this.enableLogCallback = enableLogCallback;
    }

    /**
     * @return Whether received data is being logged to a file
     */
    public boolean isEnableLogFile() {
        return enableLogFile;
    }

    /**
     * @param enableLogFile Set whether received data should be logged to a file
     */
    public void setEnableLogFile(boolean enableLogFile) {
        this.enableLogFile = enableLogFile;
    }

    /**
     * @return Whether the logging callback should be given a hex representation of data instead
     */
    public boolean isHexMode() {
        return hexMode;
    }

    /**
     * @param hexMode Set whether the logging callback should be given a hex representation of data instead
     */
    public void setHexMode(boolean hexMode) {
        this.hexMode = hexMode;
    }

    // endregion

    // region Enums

    /**
     * Commands that can be sent through the `write` function
     */
    public static final class COMMAND {
        public static final byte[] CHARGE = {123};
        public static final byte[] CLEAR_FAULT = {45};
    }

    /**
     * When to update enums for message callbacks
     */
    public enum UPDATE {
        ON_RECEIVE,
        ON_VALUE_CHANGE,
        ON_VALUE_DECREASE,
        ON_VALUE_INCREASE,
    }

    public enum STATE {
        Probably_Initializing,
        Precharge,
        Idle,
        Charging,
        Button,
        Driving,
        Fault,
    }

    // endregion

    /**
     * TeensyStream gives the ability to communicate with a Teensy 3.6 over USB serial
     * <p>
     * Given a properly formatted JSON, TeensyStream can also interpret data in 8 byte chunks as string messages
     * <p>
     * TeensyStream mainly uses callbacks as a way of communication
     *
     * @param activity     The main activity
     * @param logMessage   The callback to be used for logging a value's string interpretation to UI
     * @param deviceAttach The callback to be called when a device is attached
     * @param deviceDetach The callback to be called when a device is detached
     * @param callOnLoad   The callback to be called when a new JSON map has been updated
     */
    public TeensyStream(Activity activity, TeensyLogCallback logMessage, TeensyCallback deviceAttach, TeensyCallback deviceDetach, TeensyLogBooleanCallback callOnLoad) {
        Log.i(LOG_TAG, "Making teensy stream");
        this.callOnLoad = callOnLoad;
        loader = new JSONLoad(activity);

        try { // Set logging file
            File path = activity.getFilesDir();
            String FILENAME_LOG = "TEENSY_LOG-%s.log";
            Calendar calendar = Calendar.getInstance();
            String date = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss", Locale.US).format(calendar.getTime());
            File file = new File(path, String.format(FILENAME_LOG, date));
            logFile = new FileOutputStream(file);
            Log.i(LOG_TAG, file.getAbsolutePath());
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            Toaster.showToast("Failed to open new file for teensy logging", true);
        }

        enableLogFile = logFile != null;

        UsbSerialInterface.UsbReadCallback streamCallback = arg0 -> {
            if (enableLogFile)
                try {
                    logFile.write(arg0);
                } catch (Exception ignored) {
                }
            if (enableLogCallback) {
                String msg = processData(arg0);
                if (msg.length() > 0) {
                    logMessage.callback(msg);
                }
            } else {
                consumeData(arg0);
            }
        };

        TeensyCallback detach = () -> {
            deviceDetach.callback();
            clearValues();
        };

        serialConnection = new USBSerial(activity, streamCallback, deviceAttach, detach);

        loadLookupTable(activity);

        new android.os.Handler().postDelayed(serialConnection::open, 2000);
    }

    private void clearValues() {
        for (Map.Entry<Long, msgBlock> entry : Teensy_Data.entrySet()) {
            entry.getValue().clearValue();
        }
        currentState = -1;
    }

    // region Messaging

    /**
     * Class which deals with updating message values and callbacks
     */
    private static class msgBlock {
        private TeensyLongCallback callbackFunc; // Callback functions
        private long value = 0; // Store data
        private UPDATE updateWhen = UPDATE.ON_RECEIVE;

        void setCallback(TeensyLongCallback callback) {
            this.callbackFunc = callback;
        }

        void setUpdate(UPDATE when) {
            this.updateWhen = when;
        }

        void clearValue() {
            this.value = 0;
        }

        void update(long val) {
            long prevValue = this.value;
            this.value = val;

            if (callbackFunc != null)
                switch (updateWhen) {
                    case ON_RECEIVE:
                        callbackFunc.callback(val);
                        break;
                    case ON_VALUE_CHANGE:
                        if (prevValue != value)
                            callbackFunc.callback(val);
                        break;
                    case ON_VALUE_DECREASE:
                        if (prevValue > value)
                            callbackFunc.callback(val);
                        break;
                    case ON_VALUE_INCREASE:
                        if (prevValue < value)
                            callbackFunc.callback(val);
                }
        }

    }

    /**
     * Helper function to get the key from a map using a value
     * <p>
     * Must be a 1:1 map
     *
     * @param map   The 1:1 map to look in
     * @param value Value to find
     * @param <T>   Map key type
     * @param <E>   Map value type
     * @return type T value
     */
    private static <T, E> T getKeyByValue(Map<T, E> map, E value) {
        for (Map.Entry<T, E> entry : map.entrySet()) {
            if (Objects.equals(value, entry.getValue())) {
                return entry.getKey();
            }
        }
        return null;
    }

    /**
     * Return the value of a message
     *
     * @param msgID Runtime specific ID of a message
     * @return The last value the message received, default 0, -1 if not found
     */
    public long requestData(long msgID) {
        msgBlock msg = Teensy_Data.get(msgID);
        return msg == null ? -1 : msg.value;
    }

    /**
     * Set the callback for a specific message
     * <p>
     * Only one callback per message
     *
     * @param msgID    Runtime specific ID of a message
     * @param callback Callback class
     * @param when     When the callback should be called
     */
    public void setCallback(long msgID, TeensyLongCallback callback, UPDATE when) {
        if (Teensy_Data.containsKey(msgID)) { // Only store value if it is needed
            msgBlock msg = Teensy_Data.get(msgID);
            msg.setCallback(callback);
            msg.setUpdate(when);
        } else {
            Log.w(LOG_TAG, "Msg ID: " + msgID + " does not exist yet, no callback set");
        }
    }

    /**
     * Get the runtime specific ID of a message that will be received
     *
     * @param stringTag The exact tag that the message has
     * @param stringMsg The exact string that the message has
     * @return ID of the given message, -1 if not found
     */
    public long requestMsgID(String stringTag, String stringMsg) {
        if (!JSONLoaded) {
            Log.w(LOG_TAG, "JSON has not been loaded, unable to process request");
            Toaster.showToast("JSON has not been loaded, unable to process request");
            return -1;
        }
        Integer tagID = getKeyByValue(Teensy_LookUp_Tag, stringTag);
        Integer strID = getKeyByValue(Teensy_LookUp_Str, stringMsg);

        if (tagID != null && strID != null) {
            ByteBuffer mapping = ByteBuffer.allocate(4);
            mapping.order(ByteOrder.LITTLE_ENDIAN);
            mapping.putShort(tagID.shortValue());
            mapping.putShort(strID.shortValue());
            long msgID = mapping.getInt(0);
            Teensy_Data.put(msgID, new msgBlock());
            return msgID;
        } else {
            Log.w(LOG_TAG, "Unable to match string " + stringTag + " " + stringMsg);
            Toaster.showToast("Unable to match string " + stringTag + " " + stringMsg);
        }

        return -1;
    }

    /**
     * Set what specific messages denotes what state the teensy is in
     *
     * @param stringTag The exact tag that the message has
     * @param stringMsg The exact string that the message has
     */
    public void setStateIdentifier(String stringTag, String stringMsg) {
        long msgID = requestMsgID(stringTag, stringMsg);
        if (msgID != -1) {
            setCallback(msgID, num -> currentState = num, UPDATE.ON_VALUE_CHANGE);
        }
    }

    /**
     * Set what an STATE ENUM should be mapped to
     * If a STATE ENUM has not been mapped, getState will fail to return it
     *
     * @param stringTag The exact tag of the state
     */
    public void setStateEnum(String stringTag, STATE state) {
        if (!JSONLoaded) {
            Log.w(LOG_TAG, "JSON has not been loaded, unable to set STATE ENUM");
            Toaster.showToast("JSON has not been loaded, unable to set STATE ENUM");
            return;
        }
        Integer tagID = getKeyByValue(Teensy_LookUp_Tag, stringTag);
        Teensy_State_Map.put(Long.valueOf(tagID), state);
    }

    /**
     * Get the current presumed state the teensy is in
     *
     * @return STATE ENUM
     */
    public STATE getState() {
        if (!Teensy_State_Map.containsKey(currentState))
            return STATE.Probably_Initializing;
        return Teensy_State_Map.get(currentState);
    }

    // endregion

    // region Data Processing

    /**
     * Update requested values and run callbacks
     *
     * @param data_block 8 byte data block
     * @return the separate IDs and values of a message
     */
    private long[] updateData(byte[] data_block) {
        long[] IDs = ByteSplit.getTeensyMsg(data_block);
        long msgID = IDs[3];
        if (Teensy_Data.containsKey(msgID)) { // Only store value if it is needed
            msgBlock msg = Teensy_Data.get(msgID);
            msg.update(IDs[2]);
        }
        return IDs;
    }

    /**
     * Consume data, does not interpret anything about it
     * <p>
     * Should be faster than processData, but does not output anything
     *
     * @param raw_data received byte array
     */
    private void consumeData(byte[] raw_data) {
        for (int i = 0; i < raw_data.length; i += 8) {
            byte[] data_block = new byte[8];
            try {
                System.arraycopy(raw_data, i, data_block, 0, 8);
            } catch (ArrayIndexOutOfBoundsException e) {
                Log.w(LOG_TAG, "Received cutoff array");
                continue;
            }
            updateData(data_block);
        }
    }

    /**
     * Both Consume and interpret raw data that has been received
     *
     * @param raw_data received byte array
     * @return The interpreted string of the data
     */
    private String processData(byte[] raw_data) { // Improve: run this on separate thread
        StringBuilder output = new StringBuilder(32);

        if (hexMode) {
            for (int i = 0; i < raw_data.length; i += 8) {
                byte[] data_block = new byte[8];
                try {
                    System.arraycopy(raw_data, i, data_block, 0, 8);
                } catch (ArrayIndexOutOfBoundsException e) {
                    Log.w(LOG_TAG, "Received cutoff array");
                    continue;
                }
                updateData(data_block);
                output.append(ByteSplit.hexStr(data_block)).append("\n");
            }
            if (output.length() == 0)
                return "";
            return output.substring(0, output.length() - 1);
        }

        for (int i = 0; i < raw_data.length; i += 8) {
            byte[] data_block = new byte[8];
            try {
                System.arraycopy(raw_data, i, data_block, 0, 8);
            } catch (ArrayIndexOutOfBoundsException e) {
                continue;
            }

            long[] IDs = updateData(data_block);
            int callerID = (int) IDs[0];
            int stringID = (int) IDs[1];

            output.append(Teensy_LookUp_Tag.get(callerID)).append(' ').append(Teensy_LookUp_Str.get(stringID)).append(' ').append(IDs[2]).append('\n');
        }
        if (output.length() == 0) {
            Log.w(LOG_TAG, "USB serial might be overwhelmed!");
            return output.toString();
        }
        return output.substring(0, output.length() - 1);
    }

    // endregion

    // region Serial IO

    public boolean open() {
        return serialConnection.open();
    }

    public void close() {
        serialConnection.close();
        clearValues();
    }

    public void write(byte[] buffer) {
        serialConnection.write(buffer);
    }

    // endregion

    // region File IO

    public void updateJsonMap() {
        loader.openFile();
    }

    public void onActivityResult(Activity activity, int requestCode, int resultCode, Intent resultData) {
        loader.onActivityResult(requestCode, resultCode, resultData);
        loadLookupTable(activity);
    }

    private void loadLookupTable(Activity activity) {
        if (callOnLoad != null)
            callOnLoad.callback(__loadLookupTable(activity));
        else
            __loadLookupTable(activity);
    }

    private void saveMapToSystem(Activity activity) {
        String loadedJsonStr = loader.getLoadedJsonStr();
        loader.clearLoadedJsonStr();
        if (loadedJsonStr != null) {
            File path = activity.getFilesDir();
            File file = new File(path, FILENAME_SAVE);
            PrintWriter writer;
            try {
                writer = new PrintWriter(file);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                Toaster.showToast("Failed to save map data", true);
                return;
            }
            writer.print(loadedJsonStr);
            writer.close();
        }
    }

    public void clearMapData(Activity activity) {
        File path = activity.getFilesDir();
        File file = new File(path, FILENAME_SAVE);
        if (file.delete()) {
            Teensy_LookUp_Tag = new HashMap<>();
            Teensy_LookUp_Str = new HashMap<>();
            callOnLoad.callback(false);
            loader.clearLoadedJsonStr();
            JSONLoaded = false;
            Toaster.showToast("Map data deleted");
        } else {
            Toaster.showToast("Failed to delete map data");
        }
    }

    private String loadMapFromSystem(Activity activity) throws IOException {
        File path = activity.getFilesDir();
        File file = new File(path, FILENAME_SAVE);
        StringBuilder text = new StringBuilder();
        BufferedReader br = new BufferedReader(new FileReader(file));
        String line;
        while ((line = br.readLine()) != null) {
            text.append(line);
            text.append('\n');
        }
        br.close();
        return text.toString();
    }

    private boolean __loadLookupTable(Activity activity) {
        Log.i(LOG_TAG, "Loading lookup table");

        String JSON_INPUT = loader.getLoadedJsonStr();
        if (JSON_INPUT == null) {
            if (JSONLoaded) {
                Toaster.showToast("Teensy map unchanged");
                return true;
            }
            try {
                JSON_INPUT = loadMapFromSystem(activity);
            } catch (IOException e) {
                Toaster.showToast("No Teensy map has been loaded", true);
                return false;
            }
        }
        JSONArray json;

        HashMap<Integer, String> NEW_Teensy_LookUp_Tag = new HashMap<>();
        HashMap<Integer, String> NEW_Teensy_LookUp_Str = new HashMap<>();

        try {
            json = new JSONArray(JSON_INPUT);

            JSONObject entry = json.getJSONObject(0);
            Iterator<String> keys = entry.keys();

            while (keys.hasNext()) {
                String key = keys.next();
                NEW_Teensy_LookUp_Tag.put(entry.getInt(key), key);
            }

            entry = json.getJSONObject(1);
            keys = entry.keys();

            while (keys.hasNext()) {
                String key = keys.next();
                NEW_Teensy_LookUp_Str.put(entry.getInt(key), key);
            }

        } catch (JSONException e) {
            e.printStackTrace();
            Toaster.showToast("Json does not match correct format", true);
            return JSONLoaded;
        }

        Log.i(LOG_TAG, "Json array loaded");
        if (JSONLoaded)
            Toaster.showToast("Teensy map updated");
        else
            Toaster.showToast("Loaded Teensy map");
        saveMapToSystem(activity);
        Teensy_LookUp_Tag = NEW_Teensy_LookUp_Tag;
        Teensy_LookUp_Str = NEW_Teensy_LookUp_Str;
        JSONLoaded = true;
        return true;
    }
    // endregion

    /**
     * @return The string representation of the stored teensy messages
     */
    @Override
    public String toString() {
        StringBuilder str = new StringBuilder();
        for (Map.Entry<Long, msgBlock> e : Teensy_Data.entrySet()) {
            str.append(e.getKey());
            str.append(" : ");
            str.append(e.getValue().value);
            str.append('\n');
        }
        return str.toString();
    }

}