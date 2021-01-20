package sae.iit.saedashboard;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.ToggleButton;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.tabs.TabLayout;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Timer;
import java.util.TimerTask;

//@RequiresApi(api = Build.VERSION_CODES.KITKAT)
public class MainActivity extends AppCompatActivity {
    private final static int MAX_LOG_LINE = 27;
    private final String LOG_ID = "Main Activity";
    private ToggleButton SerialToggle;
    private LinearLayout FunctionSubTab;
    private TextView SerialLog;
    private TeensyStream TStream;

    @SuppressLint("WrongViewCast")
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            getWindow().getAttributes().layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
        }

        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);

        View decorView = getWindow().getDecorView();
        int uiOptions = View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
        decorView.setSystemUiVisibility(uiOptions);

        setContentView(R.layout.activity_main);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

        //Setting up ViewPager, Adapter, and TabLayout

        ViewPager MainPager = findViewById(R.id.MainPager);
        MyPagerAdapter pagerAdapter = new MyPagerAdapter(getSupportFragmentManager());
        TabLayout tabLayout = findViewById(R.id.tabLayout);

        MainPager.setKeepScreenOn(true);
        MainPager.setAdapter(pagerAdapter);
        tabLayout.setupWithViewPager(MainPager);
        FunctionSubTab = findViewById(R.id.FunctionSubTab);
        SerialToggle = findViewById(R.id.SerialToggle);
        SerialLog = findViewById(R.id.SerialLog);

        ToggleButton JSONToggle = findViewById(R.id.Load);

        //Background update Function. MOST IMPORTANT
        Timer timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                runOnUiThread(() -> {
                    try {
                        MainTab.setSpeedometer(TeensyStream.ADD.SPEED.getValue(TStream));
                        MainTab.setPowerGauge(TeensyStream.ADD.SPEED.getValue(TStream));
                        MainTab.setBatteryLife(TeensyStream.ADD.SPEED.getValue(TStream));
                        SecondaryTab.setLeftMotorTemp("0");
                        SecondaryTab.setRightMotorTemp("0");
                        SecondaryTab.setLeftMotorContTemp("0");
                        SecondaryTab.setRightMotorContTemp("0");
                        SecondaryTab.setActiveAeroPos("0");
                        SecondaryTab.setDCBusCurrent("0");
                    } catch (NullPointerException ignored) {
                    }
                });
            }

        }, 0, 50);

        TStream = new TeensyStream(this, this::ConsoleLog, () -> SerialToggle.setChecked(true), () -> SerialToggle.setChecked(false), JSONToggle::setChecked);

        JSONToggle.setOnLongClickListener(v -> {
            TStream.clearMapData();
            return true;
        });

        Log.i(LOG_ID, "Teensy stream created");
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent resultData) {
        super.onActivityResult(requestCode, resultCode, resultData);
        TStream.onActivityResult(requestCode, resultCode, resultData);
    }

    public void onSerialToggle(View view) {
        if (SerialToggle.isChecked()) {
            SerialToggle.setChecked(TStream.open());
        } else {
            TStream.close();
        }
    }

    /**
     * Clear test console
     */
    public void ConsoleClear() { // Test this
        runOnUiThread(() -> SerialLog.setText(""));
    }

    /**
     * Log a string to test Console
     *
     * @param msg Message String
     */
    private void ConsoleLog(String msg) {
        runOnUiThread(() -> writeTerminal(SerialLog, msg + "\n"));
    }

    private void writeTerminal(final TextView tv, final String data) {
        tv.append(data);
        // Erase excessive lines
        int excessLineNumber = tv.getLineCount() - MAX_LOG_LINE;
        if (excessLineNumber > 0) {
            int eolIndex = -1;
            CharSequence charSequence = tv.getText();
            for (int i = 0; i < excessLineNumber; i++) {
                do {
                    eolIndex++;
                } while (eolIndex < charSequence.length() && charSequence.charAt(eolIndex) != '\n');
            }
            if (eolIndex < charSequence.length()) {
                tv.getEditableText().delete(0, eolIndex + 1);
            } else {
                tv.setText("");
            }
        }
    }

    public void onClickFault(View view) { // TODO: implement android to teensy communication
        TStream.write("0000".getBytes());
        Log.d(LOG_ID, "Fault Button");
    }

    public void onClickSH(View view) {
        switch (FunctionSubTab.getVisibility()) {
            case View.VISIBLE:
                Log.d(LOG_ID, "Hide functions");
                FunctionSubTab.setVisibility(View.INVISIBLE);
                break;
            case View.INVISIBLE:
                Log.d(LOG_ID, "Show functions");
                FunctionSubTab.setVisibility(View.VISIBLE);
                break;
            case View.GONE:
                break;
        }
    }

    public void onClickClear(View view) {
        ConsoleClear();
    }

    public void onClickLoad(View view) {
        TStream.updateJsonMap();
    }

    public void onModeToggle(View view) {
        TStream.setHexMode(!TStream.getHexMode());
    }

    //CSV File Writer
    public void export(View view) {
        try {
            //saving the file into device
            FileOutputStream out = openFileOutput("data.csv", Context.MODE_PRIVATE);
            out.write((TStream.dataString()).getBytes());
            out.close();
            //exporting
            Context context = getApplicationContext();
            File fileLocation = new File(getFilesDir(), "data.csv");
            Uri path = FileProvider.getUriForFile(context, "com.example.exportcsv.fileprovider", fileLocation);
            Intent fileIntent = new Intent(Intent.ACTION_SEND);
            fileIntent.setType("text/csv");
            fileIntent.putExtra(Intent.EXTRA_SUBJECT, "Data");
            fileIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            fileIntent.putExtra(Intent.EXTRA_STREAM, path);
            startActivity(Intent.createChooser(fileIntent, "Send mail"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}