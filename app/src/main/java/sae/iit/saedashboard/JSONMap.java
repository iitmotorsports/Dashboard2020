package sae.iit.saedashboard;

import android.app.Activity;
import android.content.Intent;
import android.util.Log;

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
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;

public class JSONMap {

    private final String FILENAME_SAVE = "TEENSY_JSON_MAP.json";
    private final String LOG_TAG = "JSON MAP";
    private final JSONLoad loader;
    private HashMap<Integer, String> Teensy_LookUp_Tag = new HashMap<>();
    private HashMap<Integer, String> Teensy_LookUp_Str = new HashMap<>();
    private boolean JSONLoaded = false;
    private final Runnable runOnMapLoad;
    private final Activity activity;

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

    JSONMap(Activity activity, Runnable runOnMapLoad) {
        this.activity = activity;
        loader = new JSONLoad(activity);
        this.runOnMapLoad = runOnMapLoad;
    }

    public boolean loaded() {
        return JSONLoaded;
    }

    public Integer getTagID(String stringTag) {
        return getKeyByValue(Teensy_LookUp_Tag, stringTag);
    }

    public Integer getStrID(String stringMsg) {
        return getKeyByValue(Teensy_LookUp_Str, stringMsg);
    }

    public String getTag(Integer tagID) {
        return Teensy_LookUp_Tag.get(tagID);
    }

    public String getStr(Integer strID) {
        return Teensy_LookUp_Str.get(strID);
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
            Toaster.showToast("JSON has not been loaded, unable to process request");
            return -1;
        }
        Integer tagID = getTagID(stringTag);
        Integer strID = getStrID(stringMsg);

        if (tagID != null && strID != null) {
            ByteBuffer mapping = ByteBuffer.allocate(4);
            mapping.order(ByteOrder.LITTLE_ENDIAN);
            mapping.putShort(tagID.shortValue());
            mapping.putShort(strID.shortValue());
            //            Teensy_Data.put(msgID, new TeensyStream.msgBlock());
            return mapping.getInt(0);
        } else {
            Toaster.showToast("Unable to match string " + stringTag + " " + stringMsg);
        }

        return -1;
    }

    private void saveMapToSystem(String loadedJsonStr) {
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

    public void openFile() {
        loader.openFile();
    }

    public boolean clear() {
        File path = activity.getFilesDir();
        File file = new File(path, FILENAME_SAVE);
        if (file.delete()) {
            Teensy_LookUp_Tag = new HashMap<>();
            Teensy_LookUp_Str = new HashMap<>();
            JSONLoaded = false;
            loader.clearLoadedJsonStr();
            if (runOnMapLoad != null)
                runOnMapLoad.run();
            Toaster.showToast("Map data deleted");
            return true;
        } else {
            Toaster.showToast("Failed to delete map data");
        }
        return false;
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

    public void onActivityResult(int requestCode, int resultCode, Intent resultData) {
        loader.onActivityResult(requestCode, resultCode, resultData);
        update();
    }

    public boolean update() {
        boolean B = update(loader.getLoadedJsonStr());
        loader.clearLoadedJsonStr();
        return B;
    }

    public boolean update(String raw) {
        Log.i(LOG_TAG, "Loading lookup table");

        String JSON_INPUT = raw;
        if (JSON_INPUT == null) {
            if (JSONLoaded) {
                Toaster.showToast("Teensy map unchanged");
                return true;
            }
            try {
                JSON_INPUT = loadMapFromSystem();
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
        saveMapToSystem(raw);
        Teensy_LookUp_Tag = NEW_Teensy_LookUp_Tag;
        Teensy_LookUp_Str = NEW_Teensy_LookUp_Str;
        JSONLoaded = true;
        if (runOnMapLoad != null)
            runOnMapLoad.run();
        return true;
    }
}