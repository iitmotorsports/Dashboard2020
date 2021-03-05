package sae.iit.saedashboard;

import android.graphics.Color;
import android.os.Bundle;
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
    public TextView speedometer, batteryLife, powerDisplay;
    private ImageView batteryLifeIcon;
    private ImageView checkEngine;
    private ScArcGauge powerGauge;
    private ScArcGauge powerGauge2;
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
        powerGauge2 = rootView.findViewById(R.id.powerGauge2);
        powerDisplay = rootView.findViewById(R.id.counter);
        // Clear all default features from the gauge
        powerGauge.removeAllFeatures();
        powerGauge2.removeAllFeatures();
        // Create the base notches.
        ScNotches base = (ScNotches) powerGauge.addFeature(ScNotches.class);
        base.setTag(ScGauge.BASE_IDENTIFIER);
        base.setPosition(ScFeature.Positions.INSIDE);
        base.setRepetitions(20);
        base.setWidths(25);
        base.setHeights(30, 150);
        base.setColors(Color.parseColor("#dbdfe6"));
        base = (ScNotches) powerGauge2.addFeature(ScNotches.class);
        base.setTag(ScGauge.BASE_IDENTIFIER);
        base.setPosition(ScFeature.Positions.INSIDE);
        base.setRepetitions(20);
        base.setWidths(25);
        base.setHeights(30, 150);
        base.setColors(Color.parseColor("#dbdfe6"));

        // Create the progress notches.
        ScNotches progress = (ScNotches) powerGauge.addFeature(ScNotches.class);
        progress.setTag(ScGauge.PROGRESS_IDENTIFIER);
        progress.setColors(
                Color.parseColor("#0BA60A"),
                Color.parseColor("#FEF301"),
                Color.parseColor("#EA0C01")
        );
        progress = (ScNotches) powerGauge2.addFeature(ScNotches.class);
        progress.setTag(ScGauge.PROGRESS_IDENTIFIER);
        progress.setColors(
                Color.parseColor("#0BA60A"),
                Color.parseColor("#FEF301"),
                Color.parseColor("#EA0C01")
        );

        // Set the value
        powerGauge.setHighValue(0, 0, 100);
        powerGauge2.setHighValue(0, 0, 100);
        powerDisplay.setText("0");

        return rootView;
    }

    //Updates field info
    public void setPowerGauge(long battery) {
        powerGauge.setHighValue(Math.min((float) battery / 302.4f, 1) * 100);
        powerGauge2.setHighValue(Math.min((float) battery / 302.4f, 1) * 100);
    }

    public void setBatteryLife(long battery) {
        batteryLife.setText(String.valueOf(battery).concat("%"));
        setBatImage(battery);
//        setBatImage(convertBatteryLife(battery)/302.4);
    }

    public void setPowerDisplay(long power) {
        powerDisplay.setText(String.valueOf(power));
    }

    public void setSpeedometer(long speed) {
        speedometer.setText(String.valueOf(speed));
    }

    public void setCheckEngine(Boolean state) {
        if (state) {
            checkEngine.setVisibility(View.VISIBLE);
        } else {
            checkEngine.setVisibility(View.INVISIBLE);
        }
    }

    public void setBatImage(double level) {
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
