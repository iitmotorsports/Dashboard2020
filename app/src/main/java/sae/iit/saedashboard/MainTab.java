package sae.iit.saedashboard;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioButton;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

import com.sccomponents.gauges.library.ScFeature;
import com.sccomponents.gauges.library.ScGauge;
import com.sccomponents.gauges.library.ScNotches;

public class MainTab extends Fragment {
    private TextView speedometer, batteryLife, BMSCharge;
    private RadioButton FaultLight;
    private ColorStateList BG, RED;
    private LinearGauge powerGauge;
    private LinearGauge powerGauge2;
    private LinearGauge batteryGauge;
    private LinearGauge BMSChargeGauge;

    // Creates a view that is compatible with ViewPager
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.main_tab, container, false);
        // Initializing Fields
        speedometer = rootView.findViewById(R.id.speedometer);
        batteryLife = rootView.findViewById(R.id.batteryLife);
        BMSCharge = rootView.findViewById(R.id.BMSCharge);

        // checkEngine = rootView.findViewById(R.id.checkEngine);
        // Power Gauges
        powerGauge = rootView.findViewById(R.id.powerGauge);
        powerGauge2 = rootView.findViewById(R.id.powerGauge2);
        DisplayMetrics display = this.getResources().getDisplayMetrics();
        powerGauge.getLayoutParams().width = (display.widthPixels / 2) - 1;
        powerGauge.setLayoutParams(powerGauge.getLayoutParams());
        powerGauge2.getLayoutParams().width = (display.widthPixels / 2) - 1;
        powerGauge2.setLayoutParams(powerGauge2.getLayoutParams());
        setPowerGauges(display.widthPixels / 80f);

        // Battery
        batteryGauge = rootView.findViewById(R.id.batteryGauge);
        setBatteryGauge(rootView);

        // BMS
        BMSChargeGauge = rootView.findViewById(R.id.BMSChargeGauge);
        setBMSChargeGauge();

        // Fault Light
        FaultLight = rootView.findViewById(R.id.FaultLight);
        RED = ColorStateList.valueOf(Color.parseColor("#EA0C01"));
        BG = ColorStateList.valueOf(Color.parseColor("#3A3D4F"));

        return rootView;
    }

    private void setPowerGauges(float width) {
        // Clear all default features from the gauge
        powerGauge.removeAllFeatures();  // TODO: What should these gauges actually do
        powerGauge2.removeAllFeatures();
        // Create the base notches.
        ScNotches base = (ScNotches) powerGauge.addFeature(ScNotches.class);
        base.setTag(ScGauge.BASE_IDENTIFIER);
        base.setPosition(ScFeature.Positions.INSIDE);
        base.setRepetitions(30);
        base.setWidths(width, width * 0.6f, width * 0.8f);
        base.setHeights(30, 150);
        base.setColors(Color.parseColor("#3A3D4F"));
        base = (ScNotches) powerGauge2.addFeature(ScNotches.class);
        base.setTag(ScGauge.BASE_IDENTIFIER);
        base.setPosition(ScFeature.Positions.INSIDE);
        base.setRepetitions(30);
        base.setWidths(width, width * 0.6f, width * 0.8f);
        base.setHeights(30, 150);
        base.setColors(Color.parseColor("#3A3D4F"));

        // Create the progress notches.
        ScNotches progress = (ScNotches) powerGauge.addFeature(ScNotches.class);
        progress.setTag(ScGauge.PROGRESS_IDENTIFIER);
        progress.setColorsMode(ScFeature.ColorsMode.SOLID);
        progress.setColors(
                Color.parseColor("#0BA60A"),
                Color.parseColor("#FEF301"),
                Color.parseColor("#EA0C01")
        );
        progress = (ScNotches) powerGauge2.addFeature(ScNotches.class);
        progress.setTag(ScGauge.PROGRESS_IDENTIFIER);
        progress.setColorsMode(ScFeature.ColorsMode.SOLID);
        progress.setColors(
                Color.parseColor("#0BA60A"),
                Color.parseColor("#FEF301"),
                Color.parseColor("#EA0C01")
        );

        // Set the value
        powerGauge.setHighValue(0, 0, 100);
        powerGauge2.setHighValue(0, 0, 100);
    }

    private void setBatteryGauge(ViewGroup rootView) {
        // Clear all default features from the gauge
        batteryGauge.removeAllFeatures();
        batteryGauge.setAlpha(0.8f);

        // Create the base notches.
        ScNotches base = (ScNotches) batteryGauge.addFeature(ScNotches.class);
        base.setTag(ScGauge.BASE_IDENTIFIER);
        base.setPosition(ScFeature.Positions.MIDDLE);
        base.setRepetitions(100);
        base.setWidths(5);
        base.setHeights(250);
        base.snapToNotches(1f);
        base.setColors(Color.parseColor("#3A3D4F"));

        // Create the progress notches.
        ScNotches progress = (ScNotches) batteryGauge.addFeature(ScNotches.class);
        progress.setTag(ScGauge.PROGRESS_IDENTIFIER);
        progress.setColors(
                Color.parseColor("#EA0C01"),
                Color.parseColor("#EA0C01"),
                Color.parseColor("#FEF301"),
                Color.parseColor("#FEF301"),
                Color.parseColor("#0BA60A"),
                Color.parseColor("#0BA60A"),
                Color.parseColor("#0BA60A"),
                Color.parseColor("#0BA60A")

        );

        // Set the value
        batteryGauge.setHighValue(0, 0, 100); // TODO: set proper battery range
    }

    private void setBMSChargeGauge() {
        // Clear all default features from the gauge
        BMSChargeGauge.removeAllFeatures();
        BMSChargeGauge.setAlpha(0.8f);

        // Create the base notches.
        ScNotches base = (ScNotches) BMSChargeGauge.addFeature(ScNotches.class);
        base.setTag(ScGauge.BASE_IDENTIFIER);
        base.setPosition(ScFeature.Positions.MIDDLE);
        base.setRepetitions(100);
        base.setWidths(5);
        base.setHeights(250);
        base.snapToNotches(1f);
        base.setColors(Color.parseColor("#3A3D4F"));

        // Create the progress notches.
        ScNotches progress = (ScNotches) BMSChargeGauge.addFeature(ScNotches.class);
        progress.setTag(ScGauge.PROGRESS_IDENTIFIER);
        progress.setColors(
                Color.parseColor("#bcbdbf"),
                Color.parseColor("#0067aa")
        );

        // Set the value
        BMSChargeGauge.setHighValue(0, 0, 100); // TODO: set proper BMS Charge range
    }

    // Updates field info
    public void setFaultLight(boolean state) {
        if (state) {
            FaultLight.setChecked(true);
            FaultLight.setButtonTintList(RED);
        } else {
            FaultLight.setChecked(false);
            FaultLight.setButtonTintList(BG);
        }
    }

    public void setPowerGauge(long battery) {
        powerGauge.setHighValue(Math.min((float) battery / 302.4f, 1) * 100);
        powerGauge2.setHighValue(Math.min((float) battery / 302.4f, 1) * 100);
    }

    public void setBatteryLife(long battery) {
//        double batVal = convertBatteryLife(battery) / 302.4;
//        batteryLife.setText(String.valueOf((int) batVal).concat("%"));
        batteryLife.setText(String.valueOf(battery).concat("%"));
        batteryGauge.setHighValue((float) battery);

    }

    public void setPowerDisplay(long power) {
        BMSCharge.setText(String.valueOf(power).concat(" W"));
        BMSChargeGauge.setHighValue(power);
    }

    public void setSpeedometer(long speed) {
        speedometer.setText(String.valueOf(speed));
    }

    // TODO: Check if raw battery value received from Teensy is okay
    private static int convertBatteryLife(long battery) {
        //Assumes battery is voltage and max is 302.4V min is 216V
        return (int) ((float) ((battery - 216) / 86.4 * 100.0));
    }
}
