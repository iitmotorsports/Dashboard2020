package sae.iit.saedashboard;

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
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.ToggleButton;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.FileProvider;

import com.google.android.material.tabs.TabLayout;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.StringReader;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;
import java.util.stream.Collectors;

public class MainActivity extends AppCompatActivity {
    private final String LOG_ID = "Main Activity";
    private final int MAX_LINES = 10000;
    private ToggleButton SerialToggle;
    private LinearLayout FunctionSubTab;
    private TextView SerialLog;
    private ScrollView ConsoleScroller;
    private TeensyStream TStream;
    private MainTab mainTab;
    private SecondaryTab secondTab;
    ToggleButton ChargingSetButton;
    ConstraintLayout ConsoleLayout;
    boolean Testing = false;
    DateFormat df = new SimpleDateFormat("[HH:mm:ss]", Locale.getDefault());

    private long msgIDSpeedometer = -1;
    private long msgIDMC0Voltage = -1;
    private long msgIDMC1Voltage = -1;
    private long msgIDPowerGauge = -1;
    private long msgIDBatteryLife = -1;
    private long msgIDFault = -1;
    private long speedDv = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        // Make it fancy on newer devices
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

        Toaster.setContext(this.getBaseContext());

        //Setting up ViewPager, Adapter, and TabLayout

        CustomSwipeViewPager MainPager = findViewById(R.id.MainPager);
        MyPagerAdapter pagerAdapter = new MyPagerAdapter(getSupportFragmentManager());
        TabLayout tabLayout = findViewById(R.id.tabLayout);

        MainPager.setKeepScreenOn(true);
        MainPager.setAdapter(pagerAdapter);
        tabLayout.setupWithViewPager(MainPager);
        FunctionSubTab = findViewById(R.id.FunctionSubTab);
        SerialToggle = findViewById(R.id.SerialToggle);
        SerialLog = findViewById(R.id.SerialLog);
        ConsoleScroller = findViewById(R.id.SerialScroller);
        ConsoleLayout = findViewById(R.id.ConsoleLayout);

        findViewById(R.id.Clear).setOnLongClickListener(v -> {
            Toaster.showToast("Clearing console text", false, true);
            ConsoleHardClear();
            return false;
        });

        ChargingSetButton = findViewById(R.id.chargeSet);
        mainTab = (MainTab) pagerAdapter.getItem(0);
        secondTab = (SecondaryTab) pagerAdapter.getItem(1);

        // UI update timers
        Timer LPUITimer = new Timer();
        TimerTask LPUI_task = new TimerTask() {
            @Override
            public void run() {
                runOnUiThread(() -> {
                    if (!Testing)
                        try {
                            updateLPTabs();
                        } catch (NullPointerException ignored) {
                        }
                    else {
                        updateLPTestTabs();
                    }
                });
            }
        };
        Timer UITimer = new Timer();
        TimerTask UI_task = new TimerTask() {
            @Override
            public void run() {
                runOnUiThread(() -> {
                    if (!Testing)
                        try {
                            updateTabs();
                        } catch (NullPointerException ignored) {
                        }
                    else {
                        updateTestTabs();
                    }
                });
            }
        };
        // Clear console if it gets too big, also line counter
        TextView lineCounter = findViewById(R.id.lineCounter);
        Timer consoleTimerAsync = new Timer();
        TimerTask ConsoleTask = new TimerTask() {
            @Override
            public void run() {
                runOnUiThread(() -> {
                    int count = SerialLog.getLineCount();
                    lineCounter.setText(String.format(Locale.US, "%d/%d", count, MAX_LINES));
                    if (count > MAX_LINES)
                        ConsoleHardClear();
                });
            }
        };

        // Timer values
        consoleTimerAsync.schedule(ConsoleTask, 0, 5000);
        UITimer.schedule(UI_task, 0, 60);// TODO: How much of a delay do we really need?
        LPUITimer.schedule(LPUI_task, 0, 200);
        setupTeensyStream();
    }

    private void updateTabs() { // TODO: set appropriate UI values
        long speed = TStream.requestData(msgIDSpeedometer);
        mainTab.setSpeedometer(speed);
        mainTab.setSpeedGauge(Math.abs(speed - speedDv) * 32);
        speedDv = speed;
        mainTab.setFaultLight(TStream.requestData(msgIDFault) > 0);
        mainTab.setWaitingLight(TStream.getState() == TeensyStream.STATE.Idle);
        mainTab.setChargingLight(TStream.getState() == TeensyStream.STATE.Charging);
        mainTab.setCurrentState(TStream.getState().name().replace('_', ' '));
        ChargingSetButton.setChecked(TStream.getState() == TeensyStream.STATE.Charging);

    }

    private void updateLPTabs() {
        mainTab.setBatteryLife(TStream.requestData(msgIDBatteryLife));
        mainTab.setPowerDisplay(TStream.requestData(msgIDPowerGauge));
        secondTab.setValues(0, 0, 0, 0, 0, 0);
    }

    double testVal = 1;

    private void updateTestTabs() {
        testVal += 1;
        testVal %= 400;
        long val = (long) (testVal + (Math.random() * testVal) / 10);
        mainTab.setSpeedometer(val);
        mainTab.setSpeedGauge(val);
        mainTab.setFaultLight(val > 50);
        mainTab.setWaitingLight(val > 100);
        mainTab.setChargingLight(val > 150);

    }

    private void updateLPTestTabs() {
        long val = (long) (testVal + (Math.random() * testVal) / 10);
        mainTab.setBatteryLife(val);
        mainTab.setPowerDisplay(val);
        secondTab.setValues(val, val, val, val, val, val);
    }

    private void setupTeensyStream() {
        ToggleButton JSONToggle = findViewById(R.id.Load);

        TStream = new TeensyStream(this, this::ConsoleLog, () -> SerialToggle.setChecked(true), () -> SerialToggle.setChecked(false), JSONToggle::setChecked);

        TStream.setEnableLogCallback(false);

        JSONToggle.setOnLongClickListener(v -> {
            TStream.clearMapData(this);
            return true;
        });

        // Teensy value mapping
        // TODO: option to set these values afterwards, as there might not be a JSON mapping to start
        msgIDMC0Voltage = TStream.requestMsgID("[Front Teensy]", "[INFO]  MC0 Voltage:");
        msgIDMC1Voltage = TStream.requestMsgID("[Front Teensy]", "[INFO]  MC1 Voltage:");
        msgIDSpeedometer = TStream.requestMsgID("[Front Teensy]", "[INFO]  Current Motor Speed:");
        msgIDPowerGauge = TStream.requestMsgID("[Front Teensy]", "[INFO]  Current Power Value:");
        msgIDBatteryLife = TStream.requestMsgID("[Front Teensy]", "[INFO]  BMS State Of Charge Value:");
        msgIDFault = TStream.requestMsgID("[Front Teensy]", "[INFO]  Fault State");
        TStream.setStateIdentifier("[Front Teensy]", "[INFO]  Current State");
        TStream.setStateEnum("[Teensy Initialize]", TeensyStream.STATE.Probably_Initializing);
        TStream.setStateEnum("[PreCharge State]", TeensyStream.STATE.Precharge);
        TStream.setStateEnum("[Idle State]", TeensyStream.STATE.Idle);
        TStream.setStateEnum("[Charging State]", TeensyStream.STATE.Charging);
        TStream.setStateEnum("[Button State]", TeensyStream.STATE.Button);
        TStream.setStateEnum("[Driving Mode State]", TeensyStream.STATE.Driving);
        TStream.setStateEnum("[Fault State]", TeensyStream.STATE.Fault);

        Log.i(LOG_ID, "Teensy stream created");
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent resultData) {
        super.onActivityResult(requestCode, resultCode, resultData);
        TStream.onActivityResult(this, requestCode, resultCode, resultData);
    }

    public void onSerialToggle(View view) {
        if (SerialToggle.isChecked()) {
            if (TStream.open()) {
                SerialToggle.setChecked(true);
            } else {
                SerialToggle.setChecked(false);
                Toaster.showToast("Failed to make a connection");
            }
        } else {
            TStream.close();
        }
    }

    /**
     * Clear test console
     */
    public void ConsoleClear() {
        ConsoleScroller.smoothScrollTo(0, SerialLog.getBottom() + 50);
    }

    public void ConsoleHardClear() {
        SerialLog.setText("");
        ConsoleScroller.postDelayed(() -> ConsoleScroller.fullScroll(View.FOCUS_DOWN), 25);
    }

    private String appendTimeToEachLine(String s) {
        String prefix = df.format(new Date()).concat(" ");
        return new BufferedReader(new StringReader(s)).lines().collect(Collectors.joining('\n' + prefix, prefix, "")).concat("\n");
    }

    /**
     * Log a string to test Console
     *
     * @param msg Message String
     */
    private void ConsoleLog(String msg) {
        runOnUiThread(() -> SerialLog.append(appendTimeToEachLine(msg)));
        ConsoleScroller.postDelayed(() -> ConsoleScroller.fullScroll(View.FOCUS_DOWN), 25);
    }

    public void onConsoleSwitch(View view) {
        if (((SwitchCompat) view).isChecked()) {
            ConsoleLayout.setVisibility(View.VISIBLE);
            TStream.setEnableLogCallback(true);
            Toaster.showToast("Console Enabled", false, true);
        } else {
            ConsoleLayout.setVisibility(View.INVISIBLE);
            TStream.setEnableLogCallback(false);
            Toaster.showToast("Console Disabled", false, true);
        }
    }

    public void onTestSwitch(View view) {
        Testing = ((SwitchCompat) view).isChecked();
    }

    public void onClickFault(View view) {
        TStream.write(TeensyStream.COMMAND.CLEAR_FAULT);
    }

    public void onClickCharge(View view) {
        TStream.write(TeensyStream.COMMAND.CHARGE);
        ChargingSetButton.setChecked(false);
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

    public void onClickLock(View view) {
        LinearLayout MainFunctionTab = findViewById(R.id.MainFunctionTab);
        CustomSwipeViewPager MainPager = findViewById(R.id.MainPager);
        if (MainFunctionTab.getVisibility() == View.VISIBLE) {
            MainFunctionTab.setVisibility(View.INVISIBLE);
            MainPager.setLocked(true);
        } else {
            MainFunctionTab.setVisibility(View.VISIBLE);
            MainPager.setLocked(false);
        }
    }

    public void onClickClear(View view) {
        ConsoleClear();
    }

    public void onClickLoad(View view) {
        Toaster.showToast("Find log_lookup.json", true);
        TStream.updateJsonMap();
    }

    public void onModeToggle(View view) {
        Toaster.showToast("Changing console mode", false, true);
        TStream.setHexMode(!TStream.isHexMode());
    }

    //CSV File Writer
    public void export(View view) {
        try {
            //saving the file into device
            FileOutputStream out = openFileOutput("data.csv", Context.MODE_PRIVATE);
            out.write((TStream.toString()).getBytes());
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