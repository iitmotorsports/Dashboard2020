package sae.iit.saedashboard;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.ToggleButton;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.tabs.TabLayout;

import java.io.File;
import java.io.FileOutputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    private final String LOG_ID = "Main Activity";
    private ToggleButton SerialToggle;
    private LinearLayout FunctionSubTab;
    private TextView SerialLog;
    private ScrollView ConsoleScroller;
    private TeensyStream TStream;
    ToggleButton ChargingSetButton;
    DateFormat df = new SimpleDateFormat("[HH:mm:ss]", Locale.getDefault());
    private boolean chargingAvailable = false;

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

        Toaster.setContext(this.getBaseContext());

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
        ConsoleScroller = findViewById(R.id.SerialScroller);

        findViewById(R.id.Clear).setOnLongClickListener(v -> {
            ConsoleTest();
            return false;
        });

        ChargingSetButton = findViewById(R.id.chargeSet);
        MainTab mT = (MainTab) pagerAdapter.getItem(0);
        SecondaryTab sT = (SecondaryTab) pagerAdapter.getItem(1);

        // UI update handler
        Handler handler = new Handler();
        Runnable runnableCode = new Runnable() {
            @Override
            public void run() {
                try { // TODO: Teensy value mapping
                    mT.setSpeedometer((long) (Math.random() * 100));
                    mT.setPowerGauge((long) (Math.random() * 100));
                    mT.setBatteryLife((long) (Math.random() * 100));
                    sT.setLeftMotorTemp("0");
                    sT.setRightMotorTemp("0");
                    sT.setLeftMotorContTemp("0");
                    sT.setRightMotorContTemp("0");
                    sT.setActiveAeroPos("0");
                    sT.setDCBusCurrent("0");
                } catch (NullPointerException ignored) {
                }
                handler.postDelayed(this, 25); // TODO: How much of a delay do we really need?
            }
        };
        handler.post(runnableCode);

        setupTeensyStream();
    }

    private void setupTeensyStream() {
        ToggleButton JSONToggle = findViewById(R.id.Load);

        TStream = new TeensyStream(this, this::ConsoleLog, () -> SerialToggle.setChecked(true), () -> SerialToggle.setChecked(false), JSONToggle::setChecked);

        JSONToggle.setOnLongClickListener(v -> {
            TStream.clearMapData(this);
            return true;
        });

        // Teensy value mapping


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

    public void ConsoleTest() {
        Toaster.showToast("Printing Test String", false, true);
        for (int i = 0; i < 50; i++) ConsoleLog("Test String " + i);
    }

    /**
     * Log a string to test Console
     *
     * @param msg Message String
     */
    private void ConsoleLog(String msg) {
        runOnUiThread(() -> writeTerminal(SerialLog, String.format("%s %s\n", df.format(new Date()), msg)));
        ConsoleScroller.post(() -> ConsoleScroller.fullScroll(View.FOCUS_DOWN));
        ConsoleScroller.post(() -> ConsoleScroller.scrollTo(0, 100));
    }

    private void writeTerminal(final TextView tv, final String data) {
        tv.append(data);
    }

    public void onClickFault(View view) {
        TStream.write(TeensyStream.COMMAND.CLEAR_FAULT);
    }

    public void onClickCharge(View view) { // TODO: only allow to send signal when teensy says so
        Toaster.showToast("Ahhh!");
//        if (chargingAvailable) {
        TStream.write(TeensyStream.COMMAND.CHARGE);
//        } else {
//            ChargingSetButton.setChecked(false);
//            Toast.makeText(this.getBaseContext(), "Charging not available right now", Toast.LENGTH_SHORT).show();
//        }
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
        Toaster.showToast("Find log_lookup.json", true);
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