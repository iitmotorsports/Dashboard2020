package sae.iit.saedashboard;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.util.Log;
import android.util.Pair;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Locale;

public class DataLogTab extends Fragment {
    private Button showButton, upButton, downButton, deleteButton, deleteAllButton, updateButton, minBtn, plsBtn;
    private ScrollView fileListScroller, logScroller;
    private LinearLayout fileLayout;
    private TeensyStream stream;
    private Activity activity;
    private final ArrayList<Pair<File, TextView>> fileList = new ArrayList<>();
    private int selectedFile = -1;
    private TextView LogViewer;
    private Thread colorThread;
    private ProgressBar logWait;
    private final float[] viewTextSize = {12};

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.data_tab, container, false);

        LogViewer = rootView.findViewById(R.id.LogViewer);
        fileLayout = rootView.findViewById(R.id.FileListLayout);
        fileListScroller = rootView.findViewById(R.id.FileListScroller);
        logScroller = rootView.findViewById(R.id.logScroller);
        showButton = rootView.findViewById(R.id.showButton);
        upButton = rootView.findViewById(R.id.upButton);
        downButton = rootView.findViewById(R.id.downButton);
        deleteButton = rootView.findViewById(R.id.deleteButton);
        deleteAllButton = rootView.findViewById(R.id.deleteAllButton);
        updateButton = rootView.findViewById(R.id.updateButton);
        minBtn = rootView.findViewById(R.id.minBtn);
        plsBtn = rootView.findViewById(R.id.plsBtn);
        logWait = rootView.findViewById(R.id.logWait);

        showButton.setOnClickListener(v -> onClickShowFile());
        upButton.setOnClickListener(v -> onClickUp());
        downButton.setOnClickListener(v -> onClickDown());
        deleteButton.setOnClickListener(v -> onClickDelete());
        deleteAllButton.setOnClickListener(v -> onClickDeleteAll());
        updateButton.setOnClickListener(v -> updateFiles());
        minBtn.setOnClickListener(v -> {
            if (logScroller.getVisibility() != View.GONE) {
                viewTextSize[0] = viewTextSize[0] - 1;
                LogViewer.setTextSize(viewTextSize[0]);
            }
        });
        plsBtn.setOnClickListener(v -> {
            if (logScroller.getVisibility() != View.GONE) {
                viewTextSize[0] = viewTextSize[0] + 1;
                LogViewer.setTextSize(viewTextSize[0]);
            }
        });

        updateFiles();
        return rootView;
    }

    public void onClickShowFile() {
        if (colorThread.isAlive()) {
            Toaster.showToast("Must wait for previous query");
            return;
        }
        if (logScroller.getVisibility() != View.GONE) {
            activity.runOnUiThread(() -> {
                showButton.setText(R.string.show);
                LogViewer.setText("");
                logScroller.setVisibility(View.GONE);
            });
        } else {
            if (selectedFile >= 0 && selectedFile < fileList.size()) {
                showFile(fileList.get(selectedFile).first);
                activity.runOnUiThread(() -> {
                    logScroller.setVisibility(View.VISIBLE);
                    showButton.setText(R.string.hide);
                });
            } else {
                Toaster.showToast("No file selected");
            }
        }
    }

    public void onClickUp() {
        if (logScroller.getVisibility() != View.GONE) {
            double v = 24 * viewTextSize[0];
            logScroller.smoothScrollBy(0, (int) (-TypedValue.COMPLEX_UNIT_SP * v));
            return;
        }
        selectFile(selectedFile - 1);
    }

    public void onClickDown() {
        if (logScroller.getVisibility() != View.GONE) {
            double v = 24 * viewTextSize[0];
            logScroller.smoothScrollBy(0, (int) (TypedValue.COMPLEX_UNIT_SP * v));
            return;
        }
        selectFile(selectedFile + 1);
    }

    public void onClickDelete() {
        if (logScroller.getVisibility() != View.GONE) {
            Toaster.showToast("Can't delete while viewing log");
            return;
        }
        if (selectedFile < 0 || selectedFile >= fileList.size()) {
            Toaster.showToast("No file selected");
            return;
        }
        if (confirm()) {
            Pair<File, TextView> p = fileList.get(selectedFile);
            if (fileDelete(p.first)) {
                fileList.remove(selectedFile);
                fileLayout.removeView(p.second);
                onClickDown();
            } else {
                Toaster.showToast("Failed to delete file");
                updateFiles();
            }
        }
    }

    public void onClickDeleteAll() {
        if (logScroller.getVisibility() != View.GONE) {
            Toaster.showToast("Can't delete while viewing log");
            return;
        }
        if (confirm()) {
            for (Pair<File, TextView> p : fileList) {
                if (!fileDelete(p.first))
                    Log.w("Data", "Failed to delete file" + p.first.getName());
                else
                    fileLayout.removeView(p.second);
            }
            fileList.clear();
            updateFiles();
        }
    }

    private boolean confirm() {
        return true;
    }

    private TextView listFile(File file, int pos) {
        String name = file.getName();
        TextView textView = new TextView(getContext());
        textView.setPadding(10, 10, 10, 10);
        textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f);
        textView.setOnClickListener(v -> selectFile(file));
        SpannableStringBuilder sb = new SpannableStringBuilder();
        String KB = String.valueOf(file.length() / 1000);
        int color = file.equals(stream.getLogFile()) ? Color.parseColor("#FEF301") : Color.parseColor("#3A3D4F");
        sb.append(TeensyStream.getColoredString(String.format(Locale.US, "%1$3s  ", pos).replace(" ", "  "), color));
        sb.append(name);
        sb.append(TeensyStream.getColoredString("  -  " + KB + " kb", color));
        activity.runOnUiThread(() -> {
                    textView.setText(sb);
                    fileLayout.addView(textView);
                }
        );
        return textView;
    }

    private void selectFile(File file) {
        int i = 0;
        int b = -1;
        for (Pair<File, TextView> ignored : fileList) {
            if (fileList.get(i).first.equals(file)) {
                b = i;
            } else {
                i++;
            }
        }
        newSelectedPos(b);
    }

    private void selectFile(int pos) {
        newSelectedPos(pos);
    }

    private void newSelectedPos(int pos) {
        int size = fileList.size();
        if (size == 0) {
            selectedFile = -1;
            return;
        }
        if (pos < 0)
            pos = size - 1;
        else if (pos >= size)
            pos = 0;
        if (selectedFile > -1 && selectedFile < size)
            fileList.get(selectedFile).second.setBackgroundColor(Color.TRANSPARENT);
        TextView view = fileList.get(pos).second;
        view.setBackgroundColor(Color.BLACK);
        int finalPos = pos;
        int height = view.getHeight();
        selectedFile = pos;
        fileListScroller.postDelayed(() -> fileListScroller.smoothScrollTo(0, (selectedFile * height) + (height * (selectedFile < finalPos ? 4 : -4))), 10);
    }

    private void showFile(File file) {
        String a = getFileString(file);
        if (colorThread == null || !colorThread.isAlive()) {
            activity.runOnUiThread(() -> logWait.setVisibility(View.VISIBLE));
            colorThread = new Thread(() -> {
                Spannable b = TeensyStream.colorMsgString(a);
                activity.runOnUiThread(() -> {
                    LogViewer.setText(b);
                    logWait.setVisibility(View.GONE);
                });
            });
            colorThread.start();
        } else {
            Toaster.showToast("Canceling previous selection");
            colorThread.interrupt();
            showFile(file);
        }
    }

    private String getFileString(File file) {
        StringBuilder sb = new StringBuilder();
        String line;
        BufferedReader reader;
        try {
            reader = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return sb.toString();
    }

    private void updateFiles() {
        if (logScroller.getVisibility() != View.GONE) {
            Toaster.showToast("Can't update while viewing log");
            return;
        }
        fileLayout.removeAllViewsInLayout();
        fileList.clear();
        String path = activity.getFilesDir().toString();
        File directory = new File(path);
        File[] files = directory.listFiles();
        int i = 0;
        if (files != null) {
            for (File file : files) {
                if (file.length() == 0) {
                    if (!fileDelete(file)) {
                        Log.w("Data", "Failed to delete empty file");
                    }
                } else {
                    String name = file.getName();
                    if (name.endsWith(".log")) {
                        fileList.add(new Pair<>(file, listFile(file, i++)));
                    }
                }
            }
        }
    }

    private boolean fileDelete(File file) {
        if (stream != null && stream.getLogFile().equals(file))
            return false;
        return file.delete();
    }

    public void setTeensyStream(TeensyStream stream, Activity activity) {
        this.stream = stream;
        this.activity = activity;
    }
}
