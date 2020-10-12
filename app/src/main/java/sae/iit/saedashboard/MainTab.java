package sae.iit.saedashboard;

import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import com.sccomponents.gauges.library.ScArcGauge;
import com.sccomponents.gauges.library.ScFeature;
import com.sccomponents.gauges.library.ScGauge;
import com.sccomponents.gauges.library.ScNotches;
import com.sccomponents.gauges.*;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import pl.pawelkleczkowski.customgauge.CustomGauge;

public class MainTab extends Fragment {
    private static TextView speedometer, batteryLife, powerDisplay;
    private static ImageView bat0, bat25, bat50, bat75, bat100;
    private ImageView checkEngine;
    private static ScArcGauge powerGauge;
    private Timer timer;

    //Creates a view that is compatible with ViewPager
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        ViewGroup rootView = (ViewGroup) inflater.inflate(
                R.layout.main_tab, container, false);
        //Initializing Fields
        speedometer = rootView.findViewById(R.id.speedometer);
        batteryLife = rootView.findViewById(R.id.batteryLife);
        bat0 = rootView.findViewById(R.id.bat0);
        bat25 = rootView.findViewById(R.id.bat25);
        bat50 = rootView.findViewById(R.id.bat50);
        bat75 = rootView.findViewById(R.id.bat75);
        bat100 = rootView.findViewById(R.id.bat100);


        //checkEngine = rootView.findViewById(R.id.checkEngine);
        //Power Gauge
        powerGauge = rootView.findViewById(R.id.powerGauge);
        powerDisplay = rootView.findViewById(R.id.counter);
        // Clear all default features from the gauge
        powerGauge.removeAllFeatures();
        // Create the base notches.
        ScNotches base = (ScNotches) powerGauge.addFeature(ScNotches.class);
        base.setTag(ScGauge.BASE_IDENTIFIER);
        base.setPosition(ScFeature.Positions.INSIDE);
        base.setRepetitions(40);
        base.setWidths(10);
        base.setHeights(10, 120);
        base.setColors(Color.parseColor("#dbdfe6"));

        // Create the progress notches.
        ScNotches progress = (ScNotches) powerGauge.addFeature(ScNotches.class);
        progress.setTag(ScGauge.PROGRESS_IDENTIFIER);
        progress.setColors(
                Color.parseColor("#0BA60A"),
                Color.parseColor("#FEF301"),
                Color.parseColor("#EA0C01")
        );

        // Set the value
        powerGauge.setHighValue(0, 0, 100);

        // Changes the text value to the gauge value
        powerGauge.setOnEventListener(new ScGauge.OnEventListener() {
            @Override
            public void onValueChange(ScGauge gauge, float lowValue, float highValue, boolean isRunning) {
                // Write the value
                int value = (int) ScGauge.percentageToValue(highValue, 0, 13000);
                powerDisplay.setText(Integer.toString(value));
            }

        });
        //End value needs to be changed to reflect max output in watts

        return rootView;
    }

    //Updates field info
    public static void setPowerGauge(long battery) {
        powerGauge.setHighValue(Math.min((float) battery/100, 1));
    }

    public static void setSpeedometer(long speed) {
        speedometer.setText(String.valueOf(speed));
    }

    public static void setBatteryLife(long battery) {
        batteryLife.setText(String.valueOf(battery));
    }

    public void setCheckEngine(Boolean fault) {
        if (fault) {
            checkEngine.setVisibility(View.VISIBLE);
        } else {
            checkEngine.setVisibility(View.INVISIBLE);
        }
    }

    public static void setBatImage(long level) {
        if (level <= 100 && level > 75) {
            bat0.setVisibility(View.INVISIBLE);
            bat25.setVisibility(View.INVISIBLE);
            bat50.setVisibility(View.INVISIBLE);
            bat75.setVisibility(View.INVISIBLE);
            bat100.setVisibility(View.VISIBLE);
        }
        if (level <= 75 && level > 50) {
            bat0.setVisibility(View.INVISIBLE);
            bat25.setVisibility(View.INVISIBLE);
            bat50.setVisibility(View.INVISIBLE);
            bat75.setVisibility(View.VISIBLE);
            bat100.setVisibility(View.INVISIBLE);
        }
        if (level <= 50 && level > 25) {
            bat0.setVisibility(View.INVISIBLE);
            bat25.setVisibility(View.INVISIBLE);
            bat50.setVisibility(View.VISIBLE);
            bat75.setVisibility(View.INVISIBLE);
            bat100.setVisibility(View.INVISIBLE);
        }
        if (level <= 25 && level > 0) {
            bat0.setVisibility(View.INVISIBLE);
            bat25.setVisibility(View.VISIBLE);
            bat50.setVisibility(View.INVISIBLE);
            bat75.setVisibility(View.INVISIBLE);
            bat100.setVisibility(View.INVISIBLE);
        }
        if (level <= 0) {
            bat0.setVisibility(View.VISIBLE);
            bat25.setVisibility(View.INVISIBLE);
            bat50.setVisibility(View.INVISIBLE);
            bat75.setVisibility(View.INVISIBLE);
            bat100.setVisibility(View.INVISIBLE);
        }

    }

    private static int convertBatteryLife(double battery) {
        //Assumes battery is voltage and max is 302.4V min is 216V
        int percentage = (int) ((battery - 216) / 86.4 * 100.0);
        return percentage;
    }
}
