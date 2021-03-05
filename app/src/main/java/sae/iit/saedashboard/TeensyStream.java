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
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;

public class TeensyStream {
    private final HashMap<Long, byte[]> Teensy_Data = new HashMap<>();
    private final String FILENAME_SAVE = "TEENSY_JSON_MAP.json";
    private final TeensyLogBooleanCallback callOnLoad;
    private final String LOG_TAG = "Teensy Stream";
    private final USBSerial serialConnection;
    private final JSONLoad loader;

    private HashMap<Integer, String> Teensy_LookUp_Tag = new HashMap<>();
    private HashMap<Integer, String> Teensy_LookUp_Str = new HashMap<>();
    private FileOutputStream logFile;

    private boolean JSONLoaded = false;
    private boolean hexLog = false;

    // region Interfaces

    public interface TeensyCallback extends USBSerial.DeviceActionCallback {
    }

    public interface TeensyLogCallback {
        void callback(String msg);
    }

    public interface TeensyLogBooleanCallback {
        void callback(boolean jsonLoaded);
    }

    // endregion

    /**
     * Commands that can be sent through the `write` function
     */
    public static final class COMMAND {
        public static final byte[] CHARGE = {123};
        public static final byte[] CLEAR_FAULT = {45};
    }

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

        UsbSerialInterface.UsbReadCallback streamCallback;

        if (logFile != null) {
            streamCallback = arg0 -> {
                try {
                    logFile.write(arg0);
                } catch (Exception ignored) {
                }
                String msg = setData(arg0);
                if (msg.length() > 0) {
                    logMessage.callback(msg);
                }
            };
        } else {
            streamCallback = arg0 -> {
                String msg = setData(arg0);
                if (msg.length() > 0) {
                    logMessage.callback(msg);
                }
            };
        }

        serialConnection = new USBSerial(activity, streamCallback, deviceAttach, deviceDetach);

        loadLookupTable(activity);
    }

    public void setHexMode(boolean enable) {
        hexLog = enable;
    }

    public boolean getHexMode() {
        return hexLog;
    }

    /**
     * Set teensy data in a HashMap given a raw byte array
     *
     * @param raw_data byte array
     */
    private String setData(byte[] raw_data) { // Improve: run this on separate thread
        StringBuilder output = new StringBuilder(32);

        if (hexLog) {
            for (int i = 0; i < raw_data.length; i += 8) {
                byte[] data_block = new byte[8];
                try {
                    System.arraycopy(raw_data, i, data_block, 0, 8);
                } catch (ArrayIndexOutOfBoundsException e) {
                    Log.w(LOG_TAG, "Received cutoff array");
                    continue;
                }
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

            long[] IDs = ByteSplit.getTeensyMsg(data_block);
            int callerID = (int) IDs[0];
            int stringID = (int) IDs[1];
            long msgID = IDs[3];

            if (Teensy_Data.containsKey(msgID)) { // Only store value if it is needed
                Teensy_Data.put(msgID, data_block);
            }

            output.append(Teensy_LookUp_Tag.get(callerID)).append(' ').append(Teensy_LookUp_Str.get(stringID)).append(' ').append(IDs[2]).append('\n');
        }
        if (output.length() == 0) {
            Log.w(LOG_TAG, "USB serial might be overwhelmed!");
            return output.toString();
        }
        return output.substring(0, output.length() - 1);
    }

    /**
     * @return The string representation of the stored teensy messages
     */
    public String dataString() {
        StringBuilder str = new StringBuilder();
        for (Map.Entry<Long, byte[]> e : Teensy_Data.entrySet()) {
            str.append(e.getKey());
            str.append(" : ");
            str.append(ByteSplit.hexStr(e.getValue()).replaceAll("..(?!$)", "$0|"));
        }
        return str.toString();
    }

    // region Serial IO

    public boolean open() {
        return serialConnection.open();
    }

    public void close() {
        serialConnection.close();
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

}