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
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.tabs.TabLayout;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Timer;
import java.util.TimerTask;

@RequiresApi(api = Build.VERSION_CODES.KITKAT)
public class MainActivity extends AppCompatActivity {
    private final static int MAX_LOG_LINE = 27;
    private final String LOG_ID = "Main Activity";
    private final MainTab mainTab = (MainTab) getSupportFragmentManager().findFragmentById(R.id.mainTab);
    private final SecondaryTab secondaryTab = (SecondaryTab) getSupportFragmentManager().findFragmentById(R.id.secondaryTab);
    private final TroubleshootTab troubleshootTab = (TroubleshootTab) getSupportFragmentManager().findFragmentById(R.id.troubleshootTab);
    private boolean shown = false;
    private Button startButton, closeButton, clearButton, clearTButton;
    private ImageButton SHButton;
    private TextView SConsole;
    private TeensyStream TStream;

    @SuppressLint("WrongViewCast")
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        //Hides the status bar.
        View decorView = getWindow().getDecorView();

        int uiOptions = View.SYSTEM_UI_FLAG_FULLSCREEN;
        decorView.setSystemUiVisibility(uiOptions);
        //Setting up ViewPager, Adapter, and TabLayout

        ViewPager viewPager = findViewById(R.id.viewPager);
        MyPagerAdapter pagerAdapter = new MyPagerAdapter(getSupportFragmentManager());
        TabLayout tabLayout = findViewById(R.id.tabLayout);

        viewPager.setKeepScreenOn(true);
        viewPager.setAdapter(pagerAdapter);
        tabLayout.setupWithViewPager(viewPager);
        tabLayout.setTabTextColors(getResources().getColor(R.color.white), getResources().getColor(R.color.white));
        startButton = findViewById(R.id.Start);
        closeButton = findViewById(R.id.Close);
        clearButton = findViewById(R.id.ClearF);
        clearTButton = findViewById(R.id.Clear);
        SHButton = findViewById(R.id.Show);
        //Makes corner Buttons invisible
        startButton.setVisibility(View.INVISIBLE);
        closeButton.setVisibility(View.INVISIBLE);
        clearButton.setVisibility(View.INVISIBLE);
        clearTButton.setVisibility(View.INVISIBLE);
        SConsole = findViewById(R.id.textView7);
        SConsole.setVisibility(View.INVISIBLE);
        setUIButtonsOn(false);

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

        TeensyStream.TeensyCallback connect = (() -> {
            this.onClickStart(startButton);
        }
        );
        TeensyStream.TeensyCallback disconnect = (() -> {
            this.onClickStop(closeButton);
        }
        );

        TStream = new TeensyStream(this, this::ConsoleLog, connect, disconnect);
        Log.i(LOG_ID, "Teensy stream created");
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent resultData) {
        super.onActivityResult(requestCode, resultCode, resultData);
        TStream.onActivityResult(requestCode, resultCode, resultData);
    }

    /**
     * Clear test console
     */
    public void ConsoleClear() { // Test this
        runOnUiThread(() -> SConsole.setText(""));
    }

    /**
     * Log a string to test Console
     *
     * @param msg Message String
     */
    private void ConsoleLog(String msg) {
        runOnUiThread(() -> writeTerminal(SConsole, msg + "\n"));
    }

    /**
     * Set whether start button should be disabled
     * and the close button to be enabled
     *
     * @param bool boolean value
     */
    public void setUIButtonsOn(boolean bool) {
        startButton.setEnabled(!bool);
        closeButton.setEnabled(bool);
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

    public void onClickStart(View view) {
        TStream.open();
        setUIButtonsOn(true);
    }

    public void onClickStop(View view) {
        TStream.close();
        setUIButtonsOn(false);
    }

    public void onClickFault(View view) { // TODO: implement android to teensy communication
        TStream.write("0000".getBytes());
        Log.d(LOG_ID, "Fault Button");
    }

    public void onClickSH(View view) {
        if (!shown) {
            startButton.setVisibility(View.VISIBLE);
            closeButton.setVisibility(View.VISIBLE);
            clearButton.setVisibility(View.VISIBLE);
            clearTButton.setVisibility(View.VISIBLE);
            SConsole.setVisibility(View.VISIBLE);
            shown = true;
        } else {
            startButton.setVisibility(View.INVISIBLE);
            closeButton.setVisibility(View.INVISIBLE);
            clearButton.setVisibility(View.INVISIBLE);
            clearTButton.setVisibility(View.INVISIBLE);
            SConsole.setVisibility(View.INVISIBLE);
            shown = false;
        }
    }

    public void onClickClear(View view) {
        ConsoleClear();
        TStream.updateJsonMap(); // TODO: move to it's own button
    }

/*	protected void onResume(Bundle savedInstanceState) {
		//Hides the status bar.
		View decorView = getWindow().getDecorView();
		int uiOptions = View.SYSTEM_UI_FLAG_FULLSCREEN;
		decorView.setSystemUiVisibility(uiOptions);
	}
    //Updates display on tablet, will receive an array
    static public void update() {
    }*/

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