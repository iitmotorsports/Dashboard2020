package sae.iit.saedashboard;

import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.RequiresApi;

import com.felhr.usbserial.UsbSerialInterface;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

@RequiresApi(api = Build.VERSION_CODES.KITKAT)
public class TeensyStream {
    private final byte[] HEX_ARRAY = "0123456789ABCDEF".getBytes(StandardCharsets.UTF_8);
    private final int ID_SIZE = 2; // How big is the teensy message ID
    private final String FILENAME_SAVE = "TEENSY_JSON_MAP.json";
    private final String LOG_TAG = "Teensy Stream";
    private final HashMap<Integer, byte[]> Teensy_Data = new HashMap<>();
    private HashMap<Integer, String> Teensy_LookUp_Tag = new HashMap<>();
    private HashMap<Long, String> Teensy_LookUp_Str = new HashMap<>();
    private final JSONLoad loader;
    private final Activity activity;
    private final USBSerial serialConnection;
    private boolean JSONLoaded = false;
    private final TeensyLogJsonLoadCallback callOnLoad;
    private boolean hexLog = false;

    public interface TeensyCallback extends USBSerial.DeviceActionCallback {
    }

    public interface TeensyLogCallback {
        void callback(String msg);
    }

    public interface TeensyLogJsonLoadCallback {
        void callback(boolean jsonLoaded);
    }

    /**
     * Enumerate the teensy addresses and define functions for each one that needs
     * a value exposed
     */
    public enum ADD { // TODO: Move enumerators to MainActivity?
        SPEED(258) {
            public long getValue(TeensyStream TS) {
                return TS.getUnsignedShort(address);
            }
            //Convert RPM to MPH
        },
        ANOTHERVAL(258) {
            public long getValue(TeensyStream TS) {
                return TS.getUnsignedInt(address, 3);
            }
        };
        public int address;

        abstract public long getValue(TeensyStream TS); // Changed to return 'Number' to return Integer or Long

        ADD(int address) {
            this.address = address;
        }
    }

    public void onActivityResult(int requestCode, int resultCode, Intent resultData) {
        loader.onActivityResult(requestCode, resultCode, resultData);
        loadLookupTable();
    }

    public void setHexMode(boolean enable) {
        hexLog = enable;
    }

    public boolean getHexMode() {
        return hexLog;
    }

    public void updateJsonMap() {
        loader.openFile();
    }

    public TeensyStream(Activity activity, TeensyLogCallback logMessage, TeensyCallback deviceAttach, TeensyCallback deviceDetach, TeensyLogJsonLoadCallback callOnLoad) {
        this.activity = activity;
        this.callOnLoad = callOnLoad;
        loader = new JSONLoad(activity);

        UsbSerialInterface.UsbReadCallback streamCallback = arg0 -> {
            String msg = setData(arg0);
            if (msg.length() > 0) {
                logMessage.callback(msg);
            }
        };

        serialConnection = new USBSerial(activity, streamCallback, deviceAttach, deviceDetach);
        Log.i(LOG_TAG, "Loading lookup table");
        loadLookupTable();
    }

    private void loadLookupTable() {
        if (callOnLoad != null)
            callOnLoad.callback(__loadLookupTable());
        else
            __loadLookupTable();
    }

    public boolean open() {
        return serialConnection.open();
    }

    public void close() {
        serialConnection.close();
    }

    public void write(byte[] buffer) {
        serialConnection.write(buffer);
    }

    private void saveMapToSystem() {
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
                Toast.makeText(activity, "Failed to save map data", Toast.LENGTH_LONG).show();
                return;
            }
            writer.print(loadedJsonStr);
            writer.close();
        }
    }

    public void clearMapData() {
        File path = activity.getFilesDir();
        File file = new File(path, FILENAME_SAVE);
        if (file.delete()) {
            Teensy_LookUp_Tag = new HashMap<>();
            Teensy_LookUp_Str = new HashMap<>();
            callOnLoad.callback(false);
            loader.clearLoadedJsonStr();
            JSONLoaded = false;
            Toast.makeText(activity, "Map data deleted", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(activity, "Failed to delete map data", Toast.LENGTH_SHORT).show();
        }
    }

    private String loadMapFromSystem() throws IOException {
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

    private boolean __loadLookupTable() {
        Log.i(LOG_TAG, "Loading lookup table");

        String JSON_INPUT = loader.getLoadedJsonStr();
        if (JSON_INPUT == null) {
            if (JSONLoaded) {
                Toast.makeText(this.activity, "Teensy map unchanged", Toast.LENGTH_SHORT).show();
                return true;
            }
            try {
                JSON_INPUT = loadMapFromSystem();
            } catch (IOException e) {
                Toast.makeText(this.activity, "No Teensy map has been loaded", Toast.LENGTH_LONG).show();
                return false;
            }
        }
        JSONArray json;

        HashMap<Integer, String> NEW_Teensy_LookUp_Tag = new HashMap<>();
        HashMap<Long, String> NEW_Teensy_LookUp_Str = new HashMap<>();

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
                NEW_Teensy_LookUp_Str.put(entry.getLong(key), key);
            }

        } catch (JSONException e) {
            e.printStackTrace();
            Toast.makeText(this.activity, "Json does not match correct format", Toast.LENGTH_LONG).show();
            return JSONLoaded;
        }

        Log.i(LOG_TAG, "Json array loaded");
        if (JSONLoaded)
            Toast.makeText(this.activity, "Teensy map updated", Toast.LENGTH_SHORT).show();
        else
            Toast.makeText(this.activity, "Loaded Teensy map", Toast.LENGTH_SHORT).show();
        saveMapToSystem();
        Teensy_LookUp_Tag = NEW_Teensy_LookUp_Tag;
        Teensy_LookUp_Str = NEW_Teensy_LookUp_Str;
        JSONLoaded = true;
        return true;
    }

    /**
     * Returns a string matching the ID codes given
     *
     * @param data raw byte array
     * @return the interpreted string
     */
    private String getLookupString(byte[] data) {
        ByteBuffer buf = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);
        long number = ((long) buf.getInt(ID_SIZE) & 0xffffffffL);
        long string = ((long) buf.getInt(ID_SIZE + 4) & 0xffffffffL);
        return Teensy_LookUp_Str.get(string) + " " + number;
    }

    /**
     * Returns the hex string representation of the given byte array
     *
     * @param bytes byte array
     * @return The hex string
     */
    public String hexStr(byte[] bytes) {
        byte[] hexChars = new byte[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = HEX_ARRAY[v >>> 4];
            hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
        }
        return new String(hexChars, StandardCharsets.UTF_8);
    }

    /**
     * Returns the hex string representation of the stored Teensy byte array
     *
     * @param MsgID Message integer ID
     * @return The hex string
     */
    public String msgHex(int MsgID) {
        byte[] bytes = Teensy_Data.get(MsgID);
        if (bytes == null)
            return "";
        return hexStr(bytes);
    }

    /**
     * Reads the first two bytes of a message's array, composing them into an
     * unsigned short value
     *
     * @param MsgID Message integer ID
     * @return The unsigned short value as an int
     */
    public int getUnsignedShort(int MsgID) {
        byte[] data = Teensy_Data.get(MsgID);
        if (data == null)
            return 0;
        return (ByteBuffer.wrap(data).getShort(ID_SIZE) & 0xffff); // offset to ignore ID bytes
    }

    /**
     * Reads two bytes at the message's index, composing them into an unsigned short
     * value.
     *
     * @param MsgID    Message integer ID
     * @param position position in message array
     * @return The unsigned short value as an int
     */
    public int getUnsignedShort(int MsgID, int position) {
        byte[] data = Teensy_Data.get(MsgID);
        if (data == null)
            return 0;
        return (ByteBuffer.wrap(data).getShort(ID_SIZE + position) & 0xffff);
    }

    /**
     * Reads the first four bytes of a message's array, composing them into an
     * unsigned int value
     *
     * @param MsgID Message integer ID
     * @return The unsigned int value as a long
     */
    public long getUnsignedInt(int MsgID) {
        byte[] data = Teensy_Data.get(MsgID);
        if (data == null)
            return 0;
        return ((long) ByteBuffer.wrap(data).getInt(ID_SIZE) & 0xffffffffL);
    }

    /**
     * Reads four bytes at the message's index, composing them into an unsigned int
     * value.
     *
     * @param MsgID    Message integer ID
     * @param position position in message array
     * @return The unsigned short value at the buffer's current position as a long
     */
    public long getUnsignedInt(int MsgID, int position) {
        byte[] data = Teensy_Data.get(MsgID);
        if (data == null)
            return 0;
        return ((long) ByteBuffer.wrap(data).getInt(ID_SIZE + position) & 0xffffffffL);
    }

    /**
     * Get the ID from the byte array received from the teensy
     *
     * @param raw_data byte array
     * @return The message ID
     */
    private int getDataID(byte[] raw_data) { // The ID 0xDEAD is 57005
        return ByteBuffer.wrap(raw_data).order(ByteOrder.LITTLE_ENDIAN).getShort() & 0xffff; // get TMsg ID
    }

    /**
     * Set teensy data in a HashMap given a raw byte array
     *
     * @param raw_data byte array
     */
    private String setData(byte[] raw_data) { // Improve: run this on separate thread
        StringBuilder output = new StringBuilder();

        if (hexLog) {
            for (int i = 0; i < raw_data.length; i += 10) {
                byte[] data_block = new byte[10];
                try {
                    System.arraycopy(raw_data, i, data_block, 0, 10);
                } catch (ArrayIndexOutOfBoundsException e) {
                    continue;
                }
                output.append(hexStr(data_block)).append("\n");
            }
            if (output.length() == 0)
                return "";
            return output.substring(0, output.length() - 1);
        }

        for (int i = 0; i < raw_data.length; i += 10) {
            byte[] data_block = new byte[10];
            try {
                System.arraycopy(raw_data, i, data_block, 0, 10);
            } catch (ArrayIndexOutOfBoundsException e) {
                continue;
            }
            int ID = getDataID(data_block);
//            Log.i(LOG_TAG, hexStr(data_block) + " | " + ID + " : " + Teensy_LookUp_Tag.get(ID) + " | " + getLookupString(data_block));
            if (Teensy_LookUp_Tag.containsKey(ID)) {
                output.append(Teensy_LookUp_Tag.get(ID)).append(" ").append(getLookupString(data_block)).append("\n");
            } else {
                Teensy_Data.put(getDataID(data_block), data_block);
            }

        }
        if (output.length() == 0) {
            Log.w(LOG_TAG, "Device might be overwhelmed!");
            return output.toString();
        }
        return output.substring(0, output.length() - 1);
    }

    /**
     * @return The string representation of the stored teensy messages
     */
    public String dataString() {
        StringBuilder str = new StringBuilder();
        for (Map.Entry<Integer, byte[]> e : Teensy_Data.entrySet()) {
            str.append(e.getKey());
            str.append(" : ");
            str.append(hexStr(e.getValue()).replaceAll("..(?!$)", "$0|"));
        }
        return str.toString();
    }
}