package sae.iit.saedashboard;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

import com.sccomponents.gauges.library.ScArcGauge;
import com.sccomponents.gauges.library.ScFeature;
import com.sccomponents.gauges.library.ScGauge;
import com.sccomponents.gauges.library.ScNotches;

import java.util.Timer;

public class MainTab extends Fragment {
    public static TextView speedometer, batteryLife, powerDisplay;
    private static ImageView batteryLifeIcon;
    private static ImageView checkEngine;
    private static ScArcGauge powerGauge;
    private Timer timer;

    //Creates a view that is compatible with ViewPager
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.main_tab, container, false);
        //Initializing Fields
        speedometer = rootView.findViewById(R.id.speedometer);
        batteryLife = rootView.findViewById(R.id.batteryLife);
        batteryLifeIcon = rootView.findViewById(R.id.batteryLifeIcon);

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
        powerDisplay.setText("0");

        // Changes the text value to the gauge value
        powerGauge.setOnEventListener((gauge, lowValue, highValue, isRunning) -> {
            // Write the value
            int value = (int) ScGauge.percentageToValue(highValue, 0, 13000);
            powerDisplay.setText(String.valueOf(value));
        });
        //End value needs to be changed to reflect max output in watts

        return rootView;
    }

    //Updates field info
    public static void setPowerGauge(long battery) {
        powerGauge.setHighValue(Math.min((float) battery / 302.4f, 1) * 100);
    }

    public static void setBatteryLife(long battery) {
        batteryLife.setText(String.valueOf(battery));
        setBatImage(battery);
//        setBatImage(convertBatteryLife(battery)/302.4);
    }

    public static void setSpeedometer(long speed) {
        speedometer.setText(String.valueOf(speed));
    }

    public static void setCheckEngine(Boolean state) {
        if (state) {
            checkEngine.setVisibility(View.VISIBLE);
        } else {
            checkEngine.setVisibility(View.INVISIBLE);
        }
    }

    public static void setBatImage(double level) {
        if (level > 75) {
            batteryLifeIcon.setImageResource(R.drawable.battery100);
        } else if (level > 50) {
            batteryLifeIcon.setImageResource(R.drawable.battery75);
        } else if (level > 25) {
            batteryLifeIcon.setImageResource(R.drawable.battery50);
        } else if (level > 0) {
            batteryLifeIcon.setImageResource(R.drawable.battery25);
        } else {
            batteryLifeIcon.setImageResource(R.drawable.battery0);
        }
    }

    // TODO: Check if raw battery value received from Teensy is okay
    private static int convertBatteryLife(long battery) {
        //Assumes battery is voltage and max is 302.4V min is 216V
        return (int) ((float) ((battery - 216) / 86.4 * 100.0));
    }
}
