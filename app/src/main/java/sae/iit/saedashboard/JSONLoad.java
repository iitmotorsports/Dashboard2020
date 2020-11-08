package sae.iit.saedashboard;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Objects;

public class JSONLoad {
    private static final int PICK_JSON_FILE = 2;
    private String loadedJsonStr;
    private Activity activity;

    public String getLoadedJsonStr(){
        return loadedJsonStr;
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
            Uri uri = null;
            if (resultData != null) {
                uri = resultData.getData();
                try {
                    loadedJsonStr = readTextFromUri(uri);
                    return;
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            Toast.makeText(activity, "Failed to load file", Toast.LENGTH_LONG).show();
        }
    }

    public void openFile(){
        try {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("text/json");
            activity.startActivityForResult(intent, PICK_JSON_FILE);
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(activity, "Failed to request a file", Toast.LENGTH_LONG).show();
        }
    }

    public JSONLoad(Activity activity){
        this.activity = activity;
    }
}
