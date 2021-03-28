package sae.iit.saedashboard;

import android.app.Activity;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class LogFileIO {
    private File activeFile;
    private FileOutputStream activeFileStream;
    private final Activity activity;
    private boolean opened = false;

    public class LogFile extends File {

        public LogFile(File file) {
            super(file.getAbsolutePath());
        }

        public LogFile(@NonNull String pathname) {
            super(pathname);
        }

        @Override
        public boolean delete() {
            if (activeFile != null && compareTo(activeFile) == 0)
                return false;
            return super.delete();
        }

        @Nullable
        @Override
        public LogFile[] listFiles() {
            File[] _files = super.listFiles();
            if (_files != null) {
                ArrayList<LogFile> files = new ArrayList<>();
                for (final File file : _files) {
                    files.add(new LogFile(file));
                }
                return files.toArray(new LogFile[0]);
            }
            return new LogFile[0];
        }
    }

    LogFileIO(Activity activity) {
        this.activity = activity;
        try {
            File path = activity.getFilesDir();
            String FILENAME_LOG = "TEENSY_LOG-%s.log";
            Calendar calendar = Calendar.getInstance();
            String date = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss", Locale.US).format(calendar.getTime());
            activeFile = new File(path, String.format(FILENAME_LOG, date));
            activeFileStream = new FileOutputStream(activeFile);
            opened = true;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            activeFile = null;
            activeFileStream = null;
            Toaster.showToast("Failed to open new file for teensy logging", true);
        }
    }

    public boolean isActiveFile(File file) {
        return file.equals(activeFile);
    }

    public boolean isOpen() {
        return opened;
    }

    public List<LogFile> listFiles() {
        String path = activity.getFilesDir().toString();
        LogFile directory = new LogFile(path);
        LogFile[] files = directory.listFiles();
        List<LogFile> fileList = new ArrayList<>();
        if (files != null) {
            for (LogFile file : files) {
                if (file.length() == 0) {
                    if (!file.delete()) {
                        Log.w("Data", "Failed to delete empty file");
                    }
                } else {
                    if (file.getName().endsWith(".log")) {
                        fileList.add(file);
                    }
                }
            }
        }
        return fileList;
    }

    public static String getString(File file) {
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

    public void write(byte[] bytes) {
        if (opened)
            try {
                activeFileStream.write(bytes);
            } catch (IOException e) {
                e.printStackTrace();
            }
    }
}
