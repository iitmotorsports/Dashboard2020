package sae.iit.saedashboard;

import android.os.Build;
import android.util.Log;
import android.widget.TextView;

import androidx.annotation.RequiresApi;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

@RequiresApi(api = Build.VERSION_CODES.KITKAT)
public class TeensyMsg {

    private static final byte[] HEX_ARRAY = "0123456789ABCDEF".getBytes(StandardCharsets.UTF_8); // Change API
    private static final int ID_SIZE = 2; // How big is the teensy message ID
    private static HashMap<Integer, byte[]> Teensy_Data = new HashMap<>();
    private static HashMap<Long, String> Teensy_LookUp_ID = new HashMap<>();
    private static HashMap<Integer, String> Teensy_LookUp_Tag = new HashMap<>();

    /*
     * Enumerate the teensy addresses and define functions for each one that needs
     * a value exposed
     */

    public enum ADD {
        SPEED(258) {
            public long getValue() {
                return getUnsignedShort(address);
            }
            //Convert RPM to MPH

        },
        ANOTHERVAL(258) {
            public long getValue() {

                return getUnsignedInt(address, 3);
            }
        };

        public int address;

        abstract public long getValue(); // Changed to return 'Number' to return Integer or Long

        ADD(int address) {
            this.address = address;
        }
    }

    public static boolean loadLookupTable() {
        // TODO: Implement json file intake
        String JSON_INPUT = "[\n" +
                "    {\n" +
                "        \"[INFO]  Finished inital Setup\": 1,\n" +
                "        \"[ LOG ] Oh why hello there\": 2,\n" +
                "        \"[ LOG ] A7 Pin Value: \": 3,\n" +
                "        \"[FATAL] UNHANDLED STATE!\": 4,\n" +
                "        \"[DEBUG] Start\": 5,\n" +
                "        \"[DEBUG] Next\": 6,\n" +
                "        \"[ERROR] State returned error code\": 7,\n" +
                "        \"[FATAL] UNABLE TO CONTINUE\": 8,\n" +
                "        \"[DEBUG] Begin setup\": 9,\n" +
                "        \"[DEBUG] Setup only once\": 10,\n" +
                "        \"[DEBUG] Finish setup\": 11\n" +
                "    },\n" +
                "    {\n" +
                "        \"Teensy Start\": 1,\n" +
                "        \"State Manager\": 2,\n" +
                "        \"UNHANDLED STATE\": 3,\n" +
                "        \"ID NOT SET\": 4\n" +
                "    }\n" +
                "]";

        JSONArray json;

        try {
            json = new JSONArray(JSON_INPUT);

            JSONObject entry = json.getJSONObject(0);
            Iterator<String> keys = entry.keys();

            while (keys.hasNext()) {
                String key = keys.next();
                Teensy_LookUp_ID.put(entry.getLong(key), key);
            }

            entry = json.getJSONObject(1);
            keys = entry.keys();

            while (keys.hasNext()) {
                String key = keys.next();
                Teensy_LookUp_Tag.put(entry.getInt(key), key);
            }

        } catch (JSONException e) {
            e.printStackTrace();
            return false;
        }

        Log.i("Json Array", "Json array loaded");
//        Log.d("Json Array", Teensy_LookUp_ID.toString());
//        Log.d("Json Array", Teensy_LookUp_Tag.toString());

        return true;
    }

    private static String getLookupString(byte data[]) {
        ByteBuffer buf = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);
        long number = ((long) buf.getInt(ID_SIZE) & 0xffffffffL);
        long string = ((long) buf.getInt(ID_SIZE + 4) & 0xffffffffL);
//        Log.i("TEENSY_LOG_MSG_ID", String.valueOf(string));
        return Teensy_LookUp_ID.get(string) + " " + String.valueOf(number);
    }

    /*
     * Returns the hex string representation of the given byte array
     *
     * @param bytes
     * @return The hex string
     */

    public static String hexStr(byte[] bytes) {
        byte[] hexChars = new byte[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = HEX_ARRAY[v >>> 4];
            hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
        }
        return new String(hexChars, StandardCharsets.UTF_8);
    }

    /*
     * Returns the hex string representation of the stored Teensy byte array
     *
     * @param MsgID
     * @return The hex string
     */
    public static String msgHex(int MsgID) {
        byte[] bytes = Teensy_Data.get(MsgID);
        if (bytes == null)
            return "";
        return hexStr(bytes);
    }

    /*
     * Reads the first two bytes of a message's array, composing them into an
     * unsigned short value
     *
     * @param MsgID
     * @return The unsigned short value as an int
     */
    public static int getUnsignedShort(int MsgID) {
        byte[] data = Teensy_Data.get(MsgID);
        if (data == null)
            return 0;
        return (ByteBuffer.wrap(data).getShort(ID_SIZE) & 0xffff); // offset to ignore ID bytes
    }

    /*
     * Reads two bytes at the message's index, composing them into an unsigned short
     * value.
     *
     * @param MsgID
     * @param position
     * @return The unsigned short value as an int
     */
    public static int getUnsignedShort(int MsgID, int position) {
        byte[] data = Teensy_Data.get(MsgID);
        if (data == null)
            return 0;
        return (ByteBuffer.wrap(data).getShort(ID_SIZE + position) & 0xffff);
    }

    /*
     * Reads the first four bytes of a message's array, composing them into an
     * unsigned int value
     *
     * @param MsgID
     * @return The unsigned int value as a long
     */
    public static long getUnsignedInt(int MsgID) {
        byte[] data = Teensy_Data.get(MsgID);
        if (data == null)
            return 0;
        return ((long) ByteBuffer.wrap(data).getInt(ID_SIZE) & 0xffffffffL);
    }

    /*
     * Reads four bytes at the message's index, composing them into an unsigned int
     * value.
     *
     * @param MsgID
     * @param position
     * @return The unsigned short value at the buffer's current position as a long
     */
    public static long getUnsignedInt(int MsgID, int position) {
        byte[] data = Teensy_Data.get(MsgID);
        if (data == null)
            return 0;
        return ((long) ByteBuffer.wrap(data).getInt(ID_SIZE + position) & 0xffffffffL);
    }

    /*
     * Get the ID from the byte array received from the teensy
     *
     * @param raw_data
     * @return The message ID
     */
    private static int getDataID(byte[] raw_data) { // The ID 0xDEAD is 57005
        return ByteBuffer.wrap(raw_data).order(ByteOrder.LITTLE_ENDIAN).getShort() & 0xffff; // get TMsg ID
    }

    /*
     * Set teensy data in a HashMap given a raw byte array
     *
     * @param raw_data
     */
    public static String setData(byte[] raw_data) { // Improve: run this on separate thread
        String output = "";
        for (int i = 0; i < raw_data.length; i += 10) {
            byte[] data_block = new byte[10];
            try {
                System.arraycopy(raw_data, i, data_block, 0, 10);
            } catch (ArrayIndexOutOfBoundsException e) {
                continue;
            }
            int ID = getDataID(data_block);
            if (Teensy_LookUp_Tag.containsKey(ID)) {
                output += Teensy_LookUp_Tag.get(ID) + " " + getLookupString(data_block) + "\n";
//                Log.i("TEENSY_LOG", Teensy_LookUp_Tag.get(ID) + " " + getLookupString(data_block));
            } else {
                Teensy_Data.put(getDataID(data_block), data_block);
            }

        }
        if (output.length() == 0) {
            Log.w("TeensyMsg", "Teensy may be overwhelming the device!");
            return output;
        }
        return output.substring(0, output.length() - 1);
    }

    /*
     * @return The string representation of the stored teensy messages
     */

    public static String dataString() {
        StringBuilder str = new StringBuilder();
        for (Map.Entry<Integer, byte[]> e : Teensy_Data.entrySet()) {
            str.append(e.getKey());
            str.append(" : ");
            str.append(hexStr(e.getValue()).replaceAll("..(?!$)", "$0|"));
        }
        return str.toString();
    }
}