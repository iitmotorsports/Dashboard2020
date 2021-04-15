package sae.iit.saedashboard;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.util.Pair;

import com.felhr.usbserial.UsbSerialInterface;

import java.io.BufferedReader;
import java.io.File;
import java.io.StringReader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

public class TeensyStream {
    private final HashMap<Long, msgBlock> Teensy_Data = new HashMap<>();
    private final String LOG_TAG = "Teensy Stream";
    private final USBSerial serialConnection;
    private final LogFileIO loggingIO;
    private final JSONMap jsonMap;
    private boolean enableLogCallback = true;
    private boolean enableLogFile;
    private MODE outputMode = MODE.ASCII;
    private final HashMap<Long, STATE> Teensy_State_Map = new HashMap<>();
    private long currentState = 0;
    private static CANDialog canAlert;
    private static EchoDialog echoAlert;
    private static final String LOG_MAP_START = "---[ LOG MAP START ]---\n";
    private static final String LOG_MAP_END = "---[ LOG MAP END ]---\n";

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
        void run(long num);
    }

    /**
     * Callback class which accepts a string, typically the string interpretation of a value
     */
    public interface TeensyLogCallback {
        void run(String msg);
    }

    /**
     * Callback class which is run whenever the JSON Map is updated
     */
    public interface TeensyInitialize {
        void run(TeensyStream TStream);
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


    public MODE getOutputMode() {
        return outputMode;
    }

    public void setOutputMode(MODE outputMode) {
        this.outputMode = outputMode;
    }

    // endregion

    // region Enums

    /**
     * Commands that can be sent through the `write` function
     */
    public static final class COMMAND {
        public static final byte[] CHARGE = {123};
        public static final byte[] SEND_CANBUS_MESSAGE = {111};
        public static final byte[] CLEAR_FAULT = {45};
        public static final byte[] TOGGLE_CANBUS_SNIFF = {127}; // TODO: implement canbus sniffer button
        public static final byte[] TOGGLE_MIRROR_MODE = {90};
        public static final byte[] ENTER_MIRROR_SET = {-1};
        public static final byte[] SEND_ECHO = {84};
    }

    public void log(String message) {
        loggingIO.write(message.getBytes());
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

    public enum MODE {
        ASCII,
        HEX,
        RAW
    }

    // endregion

    /**
     * TeensyStream gives the ability to communicate with a Teensy 3.6 over USB serial
     * <p>
     * Given a properly formatted JSON, TeensyStream can also interpret data in 8 byte chunks as string messages
     * <p>
     * TeensyStream mainly uses callbacks as a way of communication
     *
     * @param activity       The main activity
     * @param logMessage     The callback to be used for logging a value's string interpretation to UI
     * @param deviceAttach   The callback to be called when a device is attached
     * @param deviceDetach   The callback to be called when a device is detached
     * @param runOnMapChange The callback to be called when a new JSON map has been updated
     */
    public TeensyStream(Activity activity, TeensyLogCallback logMessage, TeensyCallback deviceAttach, TeensyCallback deviceDetach, JSONMap.MapChange runOnMapChange, TeensyInitialize runOnSuccessfulMapChange) {
        Log.i(LOG_TAG, "Making teensy stream");
        loggingIO = new LogFileIO(activity);

        jsonMap = new JSONMap(activity, rawJson -> {
            loggingIO.newLog();
            enableLogFile = loggingIO.isOpen();
            if (rawJson != null && enableLogFile) {
                loggingIO.write(LOG_MAP_START.getBytes());
                loggingIO.write(rawJson.getBytes());
                loggingIO.write("\n".getBytes());
                loggingIO.write(LOG_MAP_END.getBytes());
            }
            runOnSuccessfulMapChange.run(this);
        }, runOnMapChange);

        UsbSerialInterface.UsbReadCallback streamCallback = arg0 -> {
            if (enableLogFile) {
                loggingIO.write(arg0);
            }
            if (enableLogCallback) {
                String msg = processData(arg0);
                if (enableLogCallback && msg.length() > 0) {
                    logMessage.run(msg);
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

        boolean b = jsonMap.update();

        new android.os.Handler().postDelayed(serialConnection::open, 2000);
        canAlert = new CANDialog(activity, this);
        echoAlert = new EchoDialog(activity, this);
        if (b)
            runOnSuccessfulMapChange.run(this);
    }

    public LogFileIO getLoggingIO() {
        return loggingIO;
    }

    private void clearValues() {
        for (Map.Entry<Long, msgBlock> entry : Teensy_Data.entrySet()) {
            entry.getValue().clearValue();
        }
        currentState = -1;
    }

    // region Messaging

    public void showCANDialog() {
        canAlert.showDialog();
    }

    public void showEchoDialog() {
        echoAlert.showDialog();
    }

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
                        callbackFunc.run(val);
                        break;
                    case ON_VALUE_CHANGE:
                        if (prevValue != value)
                            callbackFunc.run(val);
                        break;
                    case ON_VALUE_DECREASE:
                        if (prevValue > value)
                            callbackFunc.run(val);
                        break;
                    case ON_VALUE_INCREASE:
                        if (prevValue < value)
                            callbackFunc.run(val);
                }
        }

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

    public void clear() {
        jsonMap.clear();
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
        msgBlock msg = Teensy_Data.get(msgID);
        if (msg != null) { // Only store value if it is needed
            msg.setCallback(callback);
            msg.setUpdate(when);
        } else {
            Toaster.showToast("Msg ID: " + msgID + " does not exist yet, no callback set", Toaster.STATUS.WARNING);
        }
    }

    public long requestMsgID(String stringTag, String stringMsg) {
        long msgID = jsonMap.requestMsgID(stringTag, stringMsg);
        if (msgID >= 0)
            Teensy_Data.put(msgID, new TeensyStream.msgBlock());
        else
            Toaster.showToast("Failed to request id for" + stringTag + " " + stringMsg, Toaster.STATUS.WARNING);
        return msgID;
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
        } else
            Toaster.showToast("Failed to set state id for " + stringTag + " " + stringMsg, Toaster.STATUS.WARNING);
    }

    /**
     * Set what an STATE ENUM should be mapped to
     * If a STATE ENUM has not been mapped, getState will fail to return it
     *
     * @param stringTag The exact tag of the state
     */
    public void setStateEnum(String stringTag, STATE state) {
        if (!jsonMap.loaded()) {
            Toaster.showToast("JSON has not been loaded, unable to set STATE ENUM", Toaster.STATUS.WARNING);
            return;
        }
        Integer tagID = jsonMap.getTagID(stringTag);
        if (tagID == null) {
            Toaster.showToast("Failed to set Enum for " + stringTag, Toaster.STATUS.WARNING);
            return;
        }
        long a = Long.valueOf(tagID);
        Teensy_State_Map.put(a, state);
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
        msgBlock msg = Teensy_Data.get(msgID);
        if (msg != null) { // Only store value if it is needed
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

    /***
     *
     * @param mString this will setup to your textView
     * @param colorId  text will fill with this color.
     * @return string with color, it will append to textView.
     */
    public static Spannable getColoredString(String mString, int colorId) {
        Spannable spannable = new SpannableString(mString);
        spannable.setSpan(new ForegroundColorSpan(colorId), 0, spannable.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        return spannable;
    }

    private final static List<Pair<String, Integer>> msgColors = Arrays.asList(
            new Pair<>("[INFO] ", Color.WHITE),
            new Pair<>("[DEBUG]", Color.DKGRAY),
            new Pair<>("[ERROR]", Color.RED),
            new Pair<>("[WARN] ", Color.YELLOW),
            new Pair<>("[FATAL]", Color.MAGENTA),
            new Pair<>("[ LOG ]", Color.LTGRAY)
    );

    public static int getMsgColor(String msg) {
        for (Pair<String, Integer> p : msgColors) {
            if (msg.contains(p.first))
                return p.second;
        }
        return Color.LTGRAY;
    }

    private static final Hashtable<String, Spannable> colorMsgMemo = new Hashtable<>();

    public static Spannable colorMsgString(String msg) {
        SpannableStringBuilder spannable = new SpannableStringBuilder();
        new BufferedReader(new StringReader(msg)).lines().forEachOrdered((line) -> {
            Spannable lineSpan = colorMsgMemo.get(line);
            if (lineSpan == null) {
                lineSpan = getColoredString(line + "\n", getMsgColor(line));
                colorMsgMemo.put(line, lineSpan);
            }
            spannable.append(lineSpan);
        });
        return spannable;
    }

    public static String stringifyLogFile(File file) {
        byte[] bytes = LogFileIO.getBytes(file);
        String jsonStr = LogFileIO.getString(file, LOG_MAP_END);
        int logStart = jsonStr.getBytes().length;
        StringBuilder stringFnl = new StringBuilder();
        stringFnl.append(jsonStr);

        for (int i = logStart; i < file.length(); i += 8) {
            byte[] msg = new byte[8];
            try {
                System.arraycopy(bytes, i, msg, 0, 8);
            } catch (IndexOutOfBoundsException e) {
                e.printStackTrace();
                Toaster.showToast("Warning: log file has leftover bytes", Toaster.STATUS.WARNING);
                break;
            }
            long[] IDs = ByteSplit.getTeensyMsg(msg);
            stringFnl.append(IDs[0]).append(" ").append(IDs[1]).append(" ").append(IDs[2]).append("\n");
        }

        String fnl = stringFnl.toString();
        fnl = fnl.replace("\"", "\\\"");
        fnl = fnl.replace("\n", "\\n");

        if (fnl.length() != 0) {
            return fnl;
        }

        Toaster.showToast("Returning string interpretation", Toaster.STATUS.WARNING);
        return LogFileIO.getString(file);
    }

    public static String interpretLogFile(File file) {
        byte[] bytes = LogFileIO.getBytes(file);
        JSONMap tempMap = new JSONMap();
        String jsonStr = LogFileIO.getString(file, LOG_MAP_END);
        int logStart = jsonStr.getBytes().length;
        StringBuilder stringFnl = new StringBuilder();
        if (tempMap.update(jsonStr.substring(LOG_MAP_START.length()))) {
            for (int i = logStart; i < file.length(); i += 8) {
                byte[] msg = new byte[8];
                try {
                    System.arraycopy(bytes, i, msg, 0, 8);
                } catch (IndexOutOfBoundsException e) {
                    e.printStackTrace();
                    Toaster.showToast("Warning: log file has leftover bytes", Toaster.STATUS.WARNING);
                    break;
                }
                long[] IDs = ByteSplit.getTeensyMsg(msg);
                stringFnl.append(formatMsg(tempMap.getTag((int) IDs[0]), tempMap.getStr((int) IDs[1]), IDs[2]));
            }
            String fnl = stringFnl.toString();
            if (fnl.length() != 0) {
                return fnl;
            }
        }
        Toaster.showToast("Returning string interpretation", Toaster.STATUS.WARNING);
        return LogFileIO.getString(file);
    }

    private static String formatMsg(String tagString, String msgString, long number) {
        if (tagString == null || msgString == null)
            return "";
        return tagString + ' ' + msgString + ' ' + number + '\n';
    }

    /**
     * Both Consume and interpret raw data that has been received
     *
     * @param raw_data received byte array
     * @return The interpreted string of the data
     */
    private String processData(byte[] raw_data) { // Improve: run this on separate thread
        StringBuilder output = new StringBuilder(32);

        if (outputMode == MODE.HEX) {
            for (int i = 0; i < raw_data.length; i += 8) {
                byte[] data_block = new byte[8];
                try {
                    System.arraycopy(raw_data, i, data_block, 0, 8);
                } catch (ArrayIndexOutOfBoundsException e) {
                    Log.w(LOG_TAG, "Received cutoff array");
                    continue;
                }
                updateData(data_block);
                output.append(ByteSplit.bytesToHex(data_block)).append("\n");
            }
            if (output.length() == 0)
                return "";
            return output.substring(0, output.length() - 1);
        } else if (outputMode == MODE.ASCII) {
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
                output.append(formatMsg(jsonMap.getTag(callerID), jsonMap.getStr(stringID), IDs[2]));
            }
            if (output.length() == 0) {
                Log.w(LOG_TAG, "USB serial might be overwhelmed!");
                return output.toString();
            }
            return output.substring(0, output.length() - 1);
        } else { // TODO: process data in ascii format
            return new String(raw_data);
        }
    }

    // endregion

    // region Serial IO

    public boolean isConnected() {
        return serialConnection.isConnected();
    }

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
        Toaster.showToast("Find log_lookup.json", true, Toaster.STATUS.INFO);
        jsonMap.openFile();
    }

    public void onActivityResult(int requestCode, int resultCode, Intent resultData) {
        jsonMap.onActivityResult(requestCode, resultCode, resultData);
    }

    public void updateJsonMap(String rawJSON) {
        jsonMap.update(rawJSON);
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