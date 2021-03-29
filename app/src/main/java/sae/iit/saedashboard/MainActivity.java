package sae.iit.saedashboard;

import android.content.Intent;
import android.content.pm.ActivityInfo;
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

import com.google.android.material.tabs.TabLayout;

import java.io.BufferedReader;
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
    private SwitchCompat ConsoleSwitch;
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
    private long msgIDLag = -1;
    private long msgIDBeat = -1;
    private long msgIDStartLight = -1;
    private long speedDv = 0;

    private void hideSystemUI() {
        View decorView = getWindow().getDecorView();
        int uiOptions = View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
        decorView.setSystemUiVisibility(uiOptions);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) hideSystemUI();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        // Make it fancy on newer devices
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            getWindow().getAttributes().layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
        }

        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_main);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        hideSystemUI();

        Toaster.setContext(this);

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
        ConsoleSwitch = findViewById(R.id.ConsoleSwitch);

        findViewById(R.id.Clear).setOnLongClickListener(v -> {
            Toaster.showToast("Clearing console text", false, true);
            ConsoleHardClear();
            return false;
        });

        ChargingSetButton = findViewById(R.id.chargeSet);
        mainTab = (MainTab) pagerAdapter.getItem(0);
        secondTab = (SecondaryTab) pagerAdapter.getItem(1);
        DataLogTab dataTab = (DataLogTab) pagerAdapter.getItem(2);

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
        dataTab.setTeensyStream(TStream, this);
    }

    private void updateTabs() { // TODO: set appropriate UI values
        long speed = TStream.requestData(msgIDSpeedometer);
        mainTab.setSpeedometer(speed);
        mainTab.setSpeedGauge(Math.abs(speed - speedDv) * 32);
        speedDv = speed;
        mainTab.setFaultLight(TStream.requestData(msgIDFault) > 0);
        mainTab.setWaitingLight(TStream.getState() == TeensyStream.STATE.Idle);
        mainTab.setChargingLight(TStream.getState() == TeensyStream.STATE.Charging);
        mainTab.setCurrentState(TStream.isConnected() ? TStream.getState().name().replace('_', ' ') : "");
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
        mainTab.setLagLight(val > 25, val);
        mainTab.setFaultLight(val > 50);
        mainTab.setStartLight(val % 50 <= 5);
        mainTab.setWaitingLight(val > 100);
        mainTab.setChargingLight(val > 150);
        mainTab.setCurrentState(TStream.getState().name().replace('_', ' '));
    }

    private void updateLPTestTabs() {
        long val = (long) (testVal + (Math.random() * testVal) / 10);
        mainTab.setBatteryLife(val);
        mainTab.setPowerDisplay(val);
        secondTab.setValues(val, val, val, val, val, val);
        TStream.log("whwhahahaha\n");
    }

    private void setupTeensyStream() {
        ToggleButton JSONToggle = findViewById(R.id.Load);

        TStream = new TeensyStream(this, this::ConsoleLog, () -> SerialToggle.setChecked(true), () -> {
            SerialToggle.setChecked(false);
            runOnUiThread(() -> {
                mainTab.setLagLight(false);
                mainTab.setStartLight(false);
            });
        }, JSONToggle::setChecked, (TStream) -> {
            // Teensy value mapping
            // TODO: option to set these values afterwards, as there might not be a JSON mapping to start
            msgIDMC0Voltage = TStream.requestMsgID("[Front Teensy]", "[INFO]  MC0 Voltage:");
            msgIDMC1Voltage = TStream.requestMsgID("[Front Teensy]", "[INFO]  MC1 Voltage:");
            msgIDSpeedometer = TStream.requestMsgID("[Front Teensy]", "[INFO]  Current Motor Speed:");
            msgIDPowerGauge = TStream.requestMsgID("[Front Teensy]", "[INFO]  Current Power Value:");
            msgIDBatteryLife = TStream.requestMsgID("[Front Teensy]", "[INFO]  BMS State Of Charge Value:");
            msgIDFault = TStream.requestMsgID("[Front Teensy]", "[INFO]  Fault State");
            msgIDLag = TStream.requestMsgID("[HeartBeat]", "[WARN]  Heartbeat is taking too long");
            msgIDBeat = TStream.requestMsgID("[HeartBeat]", "[INFO]  Beat");
            msgIDStartLight = TStream.requestMsgID("[Front Teensy]", "[INFO]  Start Light");
            TStream.setStateIdentifier("[Front Teensy]", "[INFO]  Current State");
            TStream.setStateEnum("[Teensy Initialize]", TeensyStream.STATE.Probably_Initializing);
            TStream.setStateEnum("[PreCharge State]", TeensyStream.STATE.Precharge);
            TStream.setStateEnum("[Idle State]", TeensyStream.STATE.Idle);
            TStream.setStateEnum("[Charging State]", TeensyStream.STATE.Charging);
            TStream.setStateEnum("[Button State]", TeensyStream.STATE.Button);
            TStream.setStateEnum("[Driving Mode State]", TeensyStream.STATE.Driving);
            TStream.setStateEnum("[Fault State]", TeensyStream.STATE.Fault);
            TStream.setCallback(msgIDStartLight, num -> runOnUiThread(() -> mainTab.setStartLight(num == 1)), TeensyStream.UPDATE.ON_VALUE_CHANGE);
            TStream.setCallback(msgIDLag, num -> runOnUiThread(() -> mainTab.setLagLight(true, num)), TeensyStream.UPDATE.ON_RECEIVE);
            TStream.setCallback(msgIDBeat, num -> runOnUiThread(() -> mainTab.setLagLight(false)), TeensyStream.UPDATE.ON_RECEIVE);
        }
        );
        TStream.setEnableLogCallback(false);
        JSONToggle.setOnLongClickListener(v -> {
            TStream.clear();
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

    public void onClickCanMsg(View view) {
        TStream.showCANDialog();
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
            SimpleAnim.animView(this, ConsoleLayout, View.VISIBLE, "fade");
            TStream.setEnableLogCallback(true);
            Toaster.showToast("Console Enabled", false, true);
        } else {
            SimpleAnim.animView(this, ConsoleLayout, View.INVISIBLE, "fade");
            TStream.setEnableLogCallback(false);
            Toaster.showToast("Console Disabled", false, true);
        }
    }

    public void onTestSwitch(View view) {
        Testing = ((SwitchCompat) view).isChecked();
        mainTab.setLagLight(Testing);
        mainTab.setStartLight(Testing);
    }

    public void onClickFault(View view) {
        TStream.write(TeensyStream.COMMAND.CLEAR_FAULT);
    }

    public void onClickCharge(View view) {
        TStream.write(TeensyStream.COMMAND.CHARGE);
        ChargingSetButton.setChecked(false);
    }

    public void onClickSH(View view) {
        if (FunctionSubTab.getAnimation() != null)
            return;
        switch (FunctionSubTab.getVisibility()) {
            case View.VISIBLE:
                Log.d(LOG_ID, "Hide functions");
                SimpleAnim.animView(this, FunctionSubTab, View.INVISIBLE, "left");
                if (ConsoleLayout.getVisibility() == View.VISIBLE) {
                    ConsoleSwitch.setChecked(false);
                    onConsoleSwitch(ConsoleSwitch);
                }
                break;
            case View.INVISIBLE:
                Log.d(LOG_ID, "Show functions");
                SimpleAnim.animView(this, FunctionSubTab, View.VISIBLE, "left");
                break;
            case View.GONE:
                break;
        }
    }

    public void onClickLock(View view) {
        LinearLayout MainUpperLayout = findViewById(R.id.MainUpperLayout);
        CustomSwipeViewPager MainPager = findViewById(R.id.MainPager);
        if (FunctionSubTab.getVisibility() == View.VISIBLE || MainUpperLayout.getVisibility() == View.VISIBLE) {
            MainPager.setLocked(true);
            SimpleAnim.animView(this, MainUpperLayout, View.INVISIBLE, "down");
            SimpleAnim.animView(this, FunctionSubTab, View.INVISIBLE, "left");
            if (ConsoleLayout.getVisibility() == View.VISIBLE) {
                ConsoleSwitch.setChecked(false);
                onConsoleSwitch(ConsoleSwitch);
            }
            ((ToggleButton) view).setChecked(true);
        } else {
            SimpleAnim.animView(this, MainUpperLayout, View.VISIBLE, "down");
            ((ToggleButton) view).setChecked(false);
            MainPager.setLocked(false);
        }
    }

    public void onClickClear(View view) {
        ConsoleClear();
    }

    public void onClickLoad(View view) {
        if (PasteAPI.checkInternetConnection(getApplicationContext())) {
            Toaster.showToast("Downloading JSON");
            PasteAPI.getLastPaste(response -> TStream.updateJsonMap(response));
        } else {
            TStream.updateJsonMap();
        }
    }

    public void onModeToggle(View view) {
        Toaster.showToast("Changing console mode", false, true);
        TStream.setHexMode(!TStream.isHexMode());
    }
}