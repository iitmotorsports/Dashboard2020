package sae.iit.saedashboard;

import android.app.Activity;
import android.app.AlertDialog;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ToggleButton;

import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

public class JSONDialog {
    private final AlertDialog dialog;

    JSONDialog(Activity activity, TeensyStream stream) {
        AlertDialog.Builder mBuilder = new AlertDialog.Builder(activity);
        View mView = activity.getLayoutInflater().inflate(R.layout.json_selection_dialog_layout, null);

        Button downloadBtn = mView.findViewById(R.id.downloadBtn);
        Button scanBtn = mView.findViewById(R.id.scanBtn);
        Button findBtn = mView.findViewById(R.id.findBtn);
        Button delBtn = mView.findViewById(R.id.delBtn);

        mBuilder.setView(mView);
        dialog = mBuilder.create();

        downloadBtn.setEnabled(PasteAPI.checkInternetConnection(activity));
        downloadBtn.setOnClickListener(v -> {
            PasteAPI.getLastJSONPaste(stream::updateJsonMap);
            dialog.dismiss();
        });
        scanBtn.setOnClickListener(v -> stream.updateQRJson());
        findBtn.setOnClickListener(v -> stream.updateJsonMap());
        delBtn.setOnClickListener(v -> stream.clear());

        mView.getViewTreeObserver().addOnGlobalLayoutListener(
                () -> dialog.getWindow().setLayout(downloadBtn.getWidth() + scanBtn.getWidth() + findBtn.getWidth() * 2 + delBtn.getWidth(), dialog.getWindow().getAttributes().height)
        );

    }

    public void showDialog() {
        if (!dialog.isShowing())
            dialog.show();
    }
}
