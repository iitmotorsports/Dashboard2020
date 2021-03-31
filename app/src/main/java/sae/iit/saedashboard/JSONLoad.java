package sae.iit.saedashboard;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Objects;

public class JSONLoad {
    private static final int PICK_JSON_FILE = 2;
    private String loadedJsonStr;
    private final Activity activity;

    public String getLoadedJsonStr() {
        return loadedJsonStr;
    }

    public void clearLoadedJsonStr() {
        loadedJsonStr = null;
    }

    public String readTextFromUri(Uri uri) throws IOException {
        StringBuilder stringBuilder = new StringBuilder();
        try (InputStream inputStream = activity.getContentResolver().openInputStream(uri);
             BufferedReader reader = new BufferedReader(
                     new InputStreamReader(Objects.requireNonNull(inputStream)))) {
            String line;
            while ((line = reader.readLine()) != null) {
                stringBuilder.append(line);
            }
        }
        return stringBuilder.toString();
    }

    public void onActivityResult(int requestCode, int resultCode, Intent resultData) {
        if (requestCode == PICK_JSON_FILE && resultCode == Activity.RESULT_OK) {
            Uri uri;
            if (resultData != null) {
                uri = resultData.getData();
                try {
                    loadedJsonStr = readTextFromUri(uri);
                    return;
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            Toaster.showToast("Failed to load file", true, Toaster.STATUS.ERROR);
        }
    }

    public void openFile() {
        try {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("*/*");
            intent.addCategory(Intent.CATEGORY_OPENABLE);

            activity.startActivityForResult(Intent.createChooser(intent, "Select a file"), PICK_JSON_FILE);
        } catch (Exception e) {
            e.printStackTrace();
            Toaster.showToast("Failed to request for file", true, Toaster.STATUS.ERROR);
        }
    }

    public JSONLoad(Activity activity) {
        this.activity = activity;
    }
}
