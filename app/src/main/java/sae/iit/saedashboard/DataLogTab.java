package sae.iit.saedashboard;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
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
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.ExecutionException;

public class DataLogTab extends Fragment {
    private Button showButton;
    private Button upButton;
    private Button downButton;
    private Button deleteButton;
    private Button exportButton;
    private Button updateButton;
    private Button minBtn;
    private Button plsBtn;
    private ScrollView fileListScroller, logScroller;
    private LinearLayout fileLayout;
    private Runnable confirm_run;
    private AlertDialog confirm_dialog;
    private TextView confirm_text;
    private LogFileIO loggingIO;
    private Activity activity;
    private final ArrayList<Pair<LogFileIO.LogFile, TextView>> fileList = new ArrayList<>();
    private int selectedFile = -1;
    private TextView LogViewer;
    private WorkManager workManager;
    private Thread colorThread;
    private ProgressBar logWait;
    private final float[] viewTextSize = {12};

    public static class formatMsgWorker extends Worker {
        public formatMsgWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
            super(context, workerParams);
        }

        @NonNull
        @Override
        public Result doWork() {
            Toaster.showToast("Done!");
            Spannable span = new SpannableStringBuilder();
            Data input = getInputData();
            @SuppressLint("RestrictedApi") Data output = new Data.Builder().put("span", span).build();

            return Result.success(output);
        }
    }

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
        exportButton = rootView.findViewById(R.id.exportButton);
        updateButton = rootView.findViewById(R.id.updateButton);
        minBtn = rootView.findViewById(R.id.minBtn);
        plsBtn = rootView.findViewById(R.id.plsBtn);
        logWait = rootView.findViewById(R.id.logWait);
        showButton.setOnClickListener(v -> onClickShowFile());
        upButton.setOnClickListener(v -> onClickUp());
        downButton.setOnClickListener(v -> onClickDown());
        deleteButton.setOnClickListener(v -> onClickDelete());
        deleteButton.setOnLongClickListener(v -> onLongClickDelete());
        exportButton.setOnClickListener(v -> onClickExport());
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

        workManager = WorkManager.getInstance(Objects.requireNonNull(getContext()));

        createConfirmDialog();
        updateFiles();
        return rootView;
    }

    private final List<OneTimeWorkRequest> requests = new ArrayList<>();

    private void clearRequests() {
        workManager.cancelAllWorkByTag("colorMsgSpanWorker");
        requests.clear();
    }

    private boolean requestsDone() {
        for (OneTimeWorkRequest request : requests) {
            if (!workManager.getWorkInfoById(request.getId()).isDone())
                return false;
        }
        return true;
    }

    private Spannable getRequests() {
        SpannableStringBuilder builder = new SpannableStringBuilder();
        try {
            for (OneTimeWorkRequest request : requests) {
                builder.append((SpannableStringBuilder) workManager.getWorkInfoById(request.getId()).get().getOutputData().getKeyValueMap().get("span"));
            }
        } catch (ExecutionException | InterruptedException e) {
            e.printStackTrace();
            return null;
        }
        return builder;
    }

    private void newRequest(String chunk) {
        Data data = new Data.Builder().putString("chunk", chunk).build();
        final OneTimeWorkRequest workRequest = new OneTimeWorkRequest.Builder(formatMsgWorker.class).addTag("colorMsgSpanWorker").setInputData(data).build();
//        workManager.getWorkInfoByIdLiveData(workRequest.getId()).observe(getViewLifecycleOwner(), workInfo -> {
//            if (workInfo != null) {
//                switch (workInfo.getState()) {
//                    case SUCCEEDED:
//                        Log.i("Worker", "Finished");
//                        return;
//                    case FAILED:
//                        Log.i("Worker", "Failed");
//                        return;
//                }
//            }
//        });
        requests.add(workRequest);
    }

    private void runRequests() {
        Toaster.showToast(String.format(Locale.US, "Running %d requests", requests.size()));
        try {
            workManager.enqueue(requests);
        } catch (IllegalArgumentException e) {
            Toaster.showToast("No requests to run");
        }
    }

    private void onClickExport() {
        if (selectedFile < 0 || selectedFile >= fileList.size()) {
            Toaster.showToast("No file selected");
            return;
        }
        loggingIO.export(TeensyStream.interpretLogFile(fileList.get(selectedFile).first));
    }

    private void createConfirmDialog() {
        AlertDialog.Builder mBuilder = new AlertDialog.Builder(activity);
        View mView = activity.getLayoutInflater().inflate(R.layout.confirm_delete_dialog, null);

        Button yes = mView.findViewById(R.id.Yesbtn);
        Button no = mView.findViewById(R.id.Nobtn);
        confirm_text = mView.findViewById(R.id.confirmText);

        mBuilder.setView(mView);
        confirm_dialog = mBuilder.create();

        yes.setOnClickListener(v -> Toaster.showToast("Hold to confirm"));
        yes.setOnLongClickListener(v -> {
            confirm_run.run();
            confirm_dialog.dismiss();
            return true;
        });

        no.setOnClickListener(v -> confirm_dialog.dismiss());

    }

    public void onClickShowFile() {
        if (colorThread != null && colorThread.isAlive()) {
            Toaster.showToast("Must wait for previous query");
            return;
        }
        if (logScroller.getVisibility() != View.GONE) {
            activity.runOnUiThread(() -> {
                showButton.setText(R.string.show);
                LogViewer.setText("");
                SimpleAnim.animView(activity, logScroller, View.GONE, "fade");
            });
        } else {
            if (selectedFile >= 0 && selectedFile < fileList.size()) {
                showFile(fileList.get(selectedFile).first);
                activity.runOnUiThread(() -> {
                    SimpleAnim.animView(activity, logScroller, View.VISIBLE, "fade");
                    showButton.setText(R.string.hide);
                });
            } else {
                Toaster.showToast("No file selected");
            }
        }
    }

    public void onClickUp() {
        if (logScroller.getVisibility() != View.GONE) {
            logScroller.smoothScrollBy(0, (int) (LogViewer.getHeight() / -8d));
            return;
        }
        selectFile(selectedFile - 1);
    }

    public void onClickDown() {
        if (logScroller.getVisibility() != View.GONE) {
            logScroller.smoothScrollBy(0, (int) (LogViewer.getHeight() / 8d));
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
        confirm(this::deleteSelected, "Delete File?");
    }

    public boolean onLongClickDelete() {
        if (logScroller.getVisibility() != View.GONE) {
            Toaster.showToast("Can't delete while viewing log");
            return true;
        }
        confirm(this::deleteAll, "Delete All Files?");
        return true;
    }

    private void deleteSelected() {
        Pair<LogFileIO.LogFile, TextView> p = fileList.get(selectedFile);
        if (p.first.delete()) {
            fileList.remove(selectedFile);
            if (fileLayout != null)
                fileLayout.removeView(p.second);
            onClickDown();
        } else {
            Toaster.showToast("Failed to delete file");
            updateFiles();
        }
    }

    private void deleteAll() {
        for (Pair<LogFileIO.LogFile, TextView> p : fileList) {
            if (!p.first.delete())
                Log.w("Data", "Failed to delete file" + p.first.getName());
            else if (fileLayout != null)
                fileLayout.removeView(p.second);
        }
        fileList.clear();
        updateFiles();
    }

    private void confirm(Runnable run, String confirmText) {
        confirm_run = run;
        if (!confirm_dialog.isShowing()) {
            activity.runOnUiThread(() -> {
                confirm_text.setText(confirmText);
                confirm_dialog.show();
            });
        }
    }

    private TextView listFile(LogFileIO.LogFile file, int pos) {
        String name = file.getFormattedName();
        TextView textView = new TextView(getContext());
        textView.setPadding(10, 10, 10, 10);
        textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f);
        textView.setOnClickListener(v -> selectFile(file));
        SpannableStringBuilder sb = new SpannableStringBuilder();
        String KB = String.valueOf(file.length() / 1000);
        int color = loggingIO.isActiveFile(file) ? getContext().getColor(R.color.colorAccent) : getContext().getColor(R.color.backgroundText);
        sb.append(TeensyStream.getColoredString(String.format(Locale.US, "%1$3s  ", pos).replace(" ", "  "), color));
        sb.append(name);
        sb.append(TeensyStream.getColoredString("  -  " + KB + " kb", color));
        activity.runOnUiThread(() -> {
                    textView.setText(sb);
                    if (fileLayout != null)
                        fileLayout.addView(textView);
                }
        );
        return textView;
    }

    private void selectFile(LogFileIO.LogFile file) {
        int i = 0;
        int b = -1;
        for (Pair<LogFileIO.LogFile, TextView> ignored : fileList) {
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
        view.setBackgroundColor(getContext().getColor(R.color.translucentBlack));
        int finalPos = pos;
        int height = view.getHeight();
        selectedFile = pos;
        if (fileListScroller != null)
            fileListScroller.postDelayed(() -> fileListScroller.smoothScrollTo(0, (selectedFile * height) + (height * (selectedFile < finalPos ? 4 : -4))), 10);
    }

    private void showFile(LogFileIO.LogFile file) {
        if (colorThread == null || !colorThread.isAlive()) {
            activity.runOnUiThread(() -> logWait.setVisibility(View.VISIBLE));
            colorThread = new Thread(() -> {
                Spannable b = TeensyStream.colorMsgString(TeensyStream.interpretLogFile(file));
                Toaster.showToast("Done interpreting");
                activity.runOnUiThread(() -> {
                    LogViewer.setText(b);
                    logWait.setVisibility(View.GONE);
                });
            });
            colorThread.start();
        } else {
            Toaster.showToast("Canceling previous selection");
            if (colorThread.isAlive())
                colorThread.interrupt();
            showFile(file);
        }
    }

    private void updateFiles() {
        if (logScroller != null && logScroller.getVisibility() != View.GONE) {
            Toaster.showToast("Can't update while viewing log");
            return;
        }
        if (fileLayout != null)
            fileLayout.removeAllViewsInLayout();
        fileList.clear();
        int i = 0;
        List<LogFileIO.LogFile> filesList = loggingIO.listFiles();
        filesList.sort(Comparator.comparingLong(LogFileIO.LogFile::lastModified).reversed());
        for (LogFileIO.LogFile file : filesList) {
            fileList.add(new Pair<>(file, listFile(file, i++)));
        }
        selectFile(Math.max(selectedFile, 0));
    }

    private void updateFileLayout() {
        fileLayout.removeAllViewsInLayout();
        for (Pair<LogFileIO.LogFile, TextView> file : fileList) {
            fileLayout.addView(file.second);
        }
    }

    public void setTeensyStream(TeensyStream stream, Activity activity) {
        this.loggingIO = stream.getLoggingIO();
        this.activity = activity;
    }
}
