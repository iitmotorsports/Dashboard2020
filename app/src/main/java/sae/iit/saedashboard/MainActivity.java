package sae.iit.saedashboard;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.ToggleButton;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import org.jetbrains.annotations.NotNull;

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
    private final int FINE_LOCATION_CODE = 1245;
    private final int MAX_LINES = 10000;
    private ToggleButton SerialToggle;
    private LinearLayout FunctionSubTab;
    private TextView SerialLog;
    private ScrollView ConsoleScroller;
    private TeensyStream TStream;
    private MainTab mainTab;
    private SecondaryTab secondTab;
    private SwitchCompat ConsoleSwitch, StreamSwitch;
    private RadioGroup conRadioGroup;
    private NearbyDataStream nearbyStream;
    ToggleButton ChargingSetButton;
    ConstraintLayout ConsoleLayout;
    DataLogTab dataTab;
    PinoutTab pinoutTab;
    boolean Testing = false;
    DateFormat df = new SimpleDateFormat("[HH:mm:ss]", Locale.getDefault());

    private long msgIDSpeedometer = -1;
    private long msgIDMC0Voltage = -1;
    private long msgIDMC1Voltage = -1;
    private long msgIDPowerGauge = -1;
    private long msgIDMC1Current = -1;
    private long msgIDMC0Current = -1;
    private long msgIDMC1BoardTemp = -1;
    private long msgIDMC0BoardTemp = -1;
    private long msgIDMC1MotorTemp = -1;
    private long msgIDMC0MotorTemp = -1;
    private long msgIDBMSHighTemp = -1;
    private long msgIDBMSLowTemp = -1;
    private long msgIDBMSDischLim = -1;
    private long msgIDBMSChrgLim = -1;
    private long msgIDBatteryLife = -1;
    private long msgIDBMSVolt = -1;
    private long msgIDBMSAmp = -1;
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
//        CustomSwipeViewPager MainPager = findViewById(R.id.MainPager);
        ViewPager2 MainPager = findViewById(R.id.MainPager);
        MyPagerAdapter pagerAdapter = new MyPagerAdapter(getSupportFragmentManager());
        TabLayout tabLayout = findViewById(R.id.tabLayout);

        MainPager.setKeepScreenOn(true);
        MainPager.setAdapter(pagerAdapter);
        MainPager.setOffscreenPageLimit(4);

        new TabLayoutMediator(tabLayout, MainPager, (tab, position) -> tab.setText(pagerAdapter.list.get(position).second)).attach();
        FunctionSubTab = findViewById(R.id.FunctionSubTab);
        SerialToggle = findViewById(R.id.SerialToggle);
        SerialLog = findViewById(R.id.SerialLog);
        ConsoleScroller = findViewById(R.id.SerialScroller);
        ConsoleLayout = findViewById(R.id.ConsoleLayout);
        ConsoleSwitch = findViewById(R.id.ConsoleSwitch);
        StreamSwitch = findViewById(R.id.StreamSwitch);
        conRadioGroup = findViewById(R.id.conRadioGroup);
        final int[] conLayWidth = {-1};
        MainPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                SideControlSize tab = (SideControlSize) pagerAdapter.list.get(position).first;
                if (conLayWidth[0] != -1 && tab != null) {
                    int newWidth = conLayWidth[0] - tab.getPanelSize();
                    if (ConsoleLayout.getWidth() != newWidth) {
                        if (ConsoleLayout.getVisibility() == View.VISIBLE) {
                            ResizeWidthAnimation anim = new ResizeWidthAnimation(ConsoleLayout, newWidth);
                            anim.setInterpolator(new AccelerateDecelerateInterpolator());
                            anim.setDuration(200);
                            ConsoleLayout.startAnimation(anim);
                        } else {
                            ConsoleLayout.getLayoutParams().width = newWidth;
                            ConsoleLayout.requestLayout();
                        }
                    }

                }
            }
        });

        ConsoleLayout.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                ConsoleLayout.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                conLayWidth[0] = ConsoleLayout.getWidth();
            }
        });

        findViewById(R.id.Clear).setOnLongClickListener(v -> {
            Toaster.showToast("Clearing console text", false, true, Toaster.STATUS.INFO);
            ConsoleHardClear();
            return false;
        });

        ChargingSetButton = findViewById(R.id.chargeSet);
        mainTab = (MainTab) pagerAdapter.list.get(0).first;
        secondTab = (SecondaryTab) pagerAdapter.list.get(1).first;
        dataTab = (DataLogTab) pagerAdapter.list.get(2).first;
        pinoutTab = (PinoutTab) pagerAdapter.list.get(3).first;

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
        assert dataTab != null;
        assert pinoutTab != null;
        nearbyStream = new NearbyDataStream(this);
        setupTeensyStream();
        nearbyStream.setReceiver(rawData -> TStream.receiveRawData(rawData));
    }

    private void streamTeensyData() {
        if (TStream.isConnected()) {
            TStream.setEnableDataMirror(true);
            if (!nearbyStream.isConnected())
                nearbyStream.broadcast();
        } else {
            if (!nearbyStream.isConnected())
                nearbyStream.receive();
        }
    }

    private void requestFineLocation() {
        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, FINE_LOCATION_CODE);
        } else {
            streamTeensyData();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String @NotNull [] permissions, int @NotNull [] grantResults) {
        if (requestCode == FINE_LOCATION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    Toaster.showToast("Permission Granted", Toaster.STATUS.SUCCESS);
                    streamTeensyData();
                }
            } else {
                Toaster.showToast("Permission Denied", Toaster.STATUS.ERROR);
            }
        }
    }


    private void updateTabs() { // TODO: set appropriate UI values
        long speed = TStream.requestData(msgIDSpeedometer);
        mainTab.setSpeedometer(speed);
        mainTab.setSpeedGauge(Math.abs(speed - speedDv) * 32);
        speedDv = speed;
        mainTab.setFaultLight(TStream.requestData(msgIDFault) > 0);
        mainTab.setWaitingLight(TStream.getState() == TeensyStream.STATE.Idle);
        mainTab.setChargingLight(TStream.getState() == TeensyStream.STATE.Charging);
        String state = TStream.getState().name();
        mainTab.setCurrentState(state.replace('_', ' '));
        ChargingSetButton.setChecked(TStream.getState() == TeensyStream.STATE.Charging);

    }

    private void updateLPTabs() {
        mainTab.setBatteryLife(TStream.requestData(msgIDBatteryLife));
//        mainTab.setPowerDisplay(TStream.requestData(msgIDPowerGauge));
        mainTab.setPowerDisplay(TStream.requestData(msgIDBMSVolt) * TStream.requestData(msgIDBMSAmp));
        secondTab.setValues(new long[]{
                TStream.requestData(msgIDMC0Voltage),
                TStream.requestData(msgIDMC0Current),
                TStream.requestData(msgIDMC1Voltage),
                TStream.requestData(msgIDMC1Current),
                TStream.requestData(msgIDBMSVolt),
                TStream.requestData(msgIDBMSAmp),
                TStream.requestData(msgIDBMSHighTemp),
                TStream.requestData(msgIDBMSLowTemp),
                TStream.requestData(msgIDBMSDischLim),
                TStream.requestData(msgIDBMSChrgLim),
        });
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
        secondTab.setValues(new long[]{val, val, val, val, val, val, val, val, val, val});
        TStream.log("test\n");
        ConsoleLog("test");
    }

    private void setupTeensyStream() {
        ToggleButton JSONToggle = findViewById(R.id.Load);

        TStream = new TeensyStream(this, this::ConsoleLog, rawData -> nearbyStream.sendPayload(rawData), () -> {
            runOnUiThread(() -> SerialToggle.setChecked(true));
            if (nearbyStream.isConnected()) {
                nearbyStream.enableBroadcast();
                TStream.setEnableDataMirror(true);
            }
        }, () -> {
            if (nearbyStream.isConnected()) {
                TStream.setEnableDataMirror(false);
                nearbyStream.enableReceive();
            }
            runOnUiThread(() -> {
                SerialToggle.setChecked(false);
                mainTab.setLagLight(false);
                mainTab.setStartLight(false);
            });
        }, JSONToggle::setChecked, (TStream) -> {
            // Teensy value mapping
            // TODO: option to set these values afterwards, as there might not be a JSON mapping to start
            msgIDMC0Voltage = TStream.requestMsgID("[Front Teensy]", "[ LOG ] MC0 DC BUS Voltage:", TeensyStream.DATA.SIGNED_SHORT);
            msgIDMC1Voltage = TStream.requestMsgID("[Front Teensy]", "[ LOG ] MC1 DC BUS Voltage:", TeensyStream.DATA.SIGNED_SHORT);
            msgIDMC1Current = TStream.requestMsgID("[Front Teensy]", "[ LOG ] MC1 DC BUS Current:", TeensyStream.DATA.SIGNED_SHORT);
            msgIDMC0Current = TStream.requestMsgID("[Front Teensy]", "[ LOG ] MC0 DC BUS Current:", TeensyStream.DATA.SIGNED_SHORT);
            msgIDMC1BoardTemp = TStream.requestMsgID("[Front Teensy]", "[ LOG ] MC1 Board Temp:", TeensyStream.DATA.SIGNED_SHORT);
            msgIDMC0BoardTemp = TStream.requestMsgID("[Front Teensy]", "[ LOG ] MC0 Board Temp:", TeensyStream.DATA.SIGNED_SHORT);
            msgIDMC1MotorTemp = TStream.requestMsgID("[Front Teensy]", "[ LOG ] MC1 Motor Temp:", TeensyStream.DATA.SIGNED_SHORT);
            msgIDMC0MotorTemp = TStream.requestMsgID("[Front Teensy]", "[ LOG ] MC0 Motor Temp:", TeensyStream.DATA.SIGNED_SHORT);
            msgIDSpeedometer = TStream.requestMsgID("[Front Teensy]", "[ LOG ] Current Motor Speed:", TeensyStream.DATA.SIGNED_INT);
            msgIDPowerGauge = TStream.requestMsgID("[Front Teensy]", "[ LOG ] MC Current Power:", TeensyStream.DATA.UNSIGNED);
            msgIDBatteryLife = TStream.requestMsgID("[Front Teensy]", "[ LOG ] BMS State Of Charge:", TeensyStream.DATA.SIGNED_BYTE);
            msgIDBMSVolt = TStream.requestMsgID("[Front Teensy]", "[ LOG ] BMS Immediate Voltage:", TeensyStream.DATA.SIGNED_SHORT);
            msgIDBMSAmp = TStream.requestMsgID("[Front Teensy]", "[ LOG ] BMS Pack Average Current:", TeensyStream.DATA.SIGNED_SHORT);
            msgIDBMSHighTemp = TStream.requestMsgID("[Front Teensy]", "[ LOG ] BMS Pack Highest Temp:", TeensyStream.DATA.UNSIGNED);
            msgIDBMSLowTemp = TStream.requestMsgID("[Front Teensy]", "[ LOG ] BMS Pack Lowest Temp:", TeensyStream.DATA.UNSIGNED);
            msgIDBMSDischLim = TStream.requestMsgID("[Front Teensy]", "[ LOG ] BMS Discharge current limit:", TeensyStream.DATA.SIGNED_SHORT);
            msgIDBMSChrgLim = TStream.requestMsgID("[Front Teensy]", "[ LOG ] BMS Charge current limit:", TeensyStream.DATA.SIGNED_SHORT);
            msgIDFault = TStream.requestMsgID("[Front Teensy]", "[ LOG ] Fault State", TeensyStream.DATA.UNSIGNED);
            msgIDLag = TStream.requestMsgID("[HeartBeat]", "[WARN]  Heartbeat is taking too long", TeensyStream.DATA.UNSIGNED);
            msgIDBeat = TStream.requestMsgID("[HeartBeat]", "[ LOG ] Beat", TeensyStream.DATA.UNSIGNED);
            msgIDStartLight = TStream.requestMsgID("[Front Teensy]", "[ LOG ] Start Light", TeensyStream.DATA.UNSIGNED);
            TStream.setStateIdentifier("[Front Teensy]", "[ LOG ] Current State", TeensyStream.DATA.UNSIGNED);
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

            dataTab.setTeensyStream(TStream, this);
            pinoutTab.setStream(TStream, this);
        }
        );
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
                Toaster.showToast("Failed to make a connection", Toaster.STATUS.ERROR);
            }
        } else {
            TStream.close();
        }
    }

    public void onClickCanMsg(View view) {
        TStream.showCANDialog();
    }

    public void onClickEcho(View view) {
        TStream.showEchoDialog();
    }

    public void onSwitchStream(View view) {
        if (StreamSwitch.isChecked()) {
            requestFineLocation();
        } else {
            TStream.setEnableDataMirror(false);
            nearbyStream.stop();
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

    private void setConsole(boolean state) {
        if (state) {
            TStream.setEnableLogCallback(true);
            Toaster.showToast("Live Logging Enabled", false, true);
        } else {
            TStream.setEnableLogCallback(false);
            Toaster.showToast("Live Logging Disabled", false, true);
        }
    }

    public void onConsoleSwitch(View view) {
        if (((SwitchCompat) view).isChecked()) {
            SimpleAnim.animView(this, ConsoleLayout, View.VISIBLE, "fade");
        } else {
            SimpleAnim.animView(this, ConsoleLayout, View.INVISIBLE, "fade");
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

    @SuppressLint("ClickableViewAccessibility")
    public void onClickLock(View view) {
        LinearLayout MainUpperLayout = findViewById(R.id.MainUpperLayout);
        ViewPager2 MainPager = findViewById(R.id.MainPager);
        MainPager.setOnTouchListener((v, event) -> {
            SimpleAnim.shakeView(this, view);
            v.performClick();
            return true;
        });
        if (FunctionSubTab.getVisibility() == View.VISIBLE || MainUpperLayout.getVisibility() == View.VISIBLE) {
            MainPager.setUserInputEnabled(false);
            setConsole(false);
            SimpleAnim.animView(this, MainUpperLayout, View.INVISIBLE, "down");
            SimpleAnim.animView(this, FunctionSubTab, View.INVISIBLE, "left");
            if (ConsoleLayout.getVisibility() == View.VISIBLE) {
                ConsoleSwitch.setChecked(false);
                onConsoleSwitch(ConsoleSwitch);
            }
            ((ToggleButton) view).setChecked(true);
        } else {
            setConsole(true);
            SimpleAnim.animView(this, MainUpperLayout, View.VISIBLE, "down");
            ((ToggleButton) view).setChecked(false);
            MainPager.setUserInputEnabled(true);
        }
    }

    public void onClickClear(View view) {
        ConsoleClear();
    }

    public void onClickLoad(View view) {
        if (PasteAPI.checkInternetConnection(getApplicationContext())) {
            Toaster.showToast("Downloading JSON");
            PasteAPI.getLastJSONPaste(response -> TStream.updateJsonMap(response));
        } else {
            TStream.updateJsonMap();
        }
    }

    public void onModeToggle(View view) {
        int clickedRadioButton = conRadioGroup.getCheckedRadioButtonId();
        RadioButton radioButton = findViewById(clickedRadioButton);
        if (clickedRadioButton == -1) {
            Toaster.showToast("Failed to make selection", Toaster.STATUS.ERROR);
        }
        String mode = radioButton.getText().toString();
        Toaster.showToast(mode + " mode", false, true, Toaster.STATUS.INFO);
        TStream.setOutputMode(TeensyStream.MODE.valueOf(mode.toUpperCase()));
    }
}