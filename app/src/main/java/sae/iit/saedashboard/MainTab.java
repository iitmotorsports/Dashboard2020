package sae.iit.saedashboard;

import android.content.res.ColorStateList;
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

import java.util.Locale;
import java.util.Objects;

public class MainTab extends Fragment {
    private TextView speedometer, batteryLife, BMSChargeValue, currentState, lagLightText;
    private RadioButton FaultLight, waitingLight, chargingLight, lagLight, startLight;
    private ColorStateList BG, RED, YELLOW, GREEN, WHITE;
    private LinearGauge powerGauge, powerGauge2, batteryGauge, BMSChargeGauge;

    // Creates a view that is compatible with ViewPager
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.main_tab, container, false);
        // Initializing Fields
        speedometer = rootView.findViewById(R.id.speedometerValue);
        batteryLife = rootView.findViewById(R.id.rightMotorTempValue);
        BMSChargeValue = rootView.findViewById(R.id.BMSChargeValue);
        currentState = rootView.findViewById(R.id.currentState);

        // checkEngine = rootView.findViewById(R.id.checkEngine);
        // Power Gauges
        powerGauge = rootView.findViewById(R.id.powerGauge);
        powerGauge2 = rootView.findViewById(R.id.powerGauge2);
        DisplayMetrics display = this.getResources().getDisplayMetrics();
        powerGauge.getLayoutParams().width = (display.widthPixels / 2) - 1;
        powerGauge.setLayoutParams(powerGauge.getLayoutParams());
        powerGauge2.getLayoutParams().width = (display.widthPixels / 2) - 1;
        powerGauge2.setLayoutParams(powerGauge2.getLayoutParams());
        setPowerGauges(display.widthPixels / (50f * display.scaledDensity), display.heightPixels / (3f * display.scaledDensity));

        batteryLife.setTextSize(display.heightPixels / (10f * display.scaledDensity));
        BMSChargeValue.setTextSize(display.heightPixels / (10f * display.scaledDensity));
        speedometer.setTextSize(display.heightPixels / (2.5f * display.scaledDensity));

        // Battery
        batteryGauge = rootView.findViewById(R.id.rightMotorTempGauge);
        setBatteryGauge(rootView);

        // BMS
        BMSChargeGauge = rootView.findViewById(R.id.BMSChargeGauge);
        setBMSChargeGauge();

        // Lights
        FaultLight = rootView.findViewById(R.id.FaultLight);
        waitingLight = rootView.findViewById(R.id.waitingLight);
        chargingLight = rootView.findViewById(R.id.chargingLight);
        lagLight = rootView.findViewById(R.id.lagLight);
        lagLightText = rootView.findViewById(R.id.lagLightText);
        startLight = rootView.findViewById(R.id.startLight);

        // Colors
        RED = ColorStateList.valueOf(Objects.requireNonNull(getContext()).getColor(R.color.red));
        YELLOW = ColorStateList.valueOf(getContext().getColor(R.color.yellow));
        GREEN = ColorStateList.valueOf(getContext().getColor(R.color.green));
        BG = ColorStateList.valueOf(getContext().getColor(R.color.backgroundText));
        WHITE = ColorStateList.valueOf(getContext().getColor(R.color.foregroundText));
        return rootView;
    }

    private void setPowerGauges(float width, float height) {
        // Clear all default features from the gauge
        powerGauge.removeAllFeatures();  // TODO: What should these gauges actually do
        powerGauge2.removeAllFeatures();
        // Create the base notches.
        ScNotches base = (ScNotches) powerGauge.addFeature(ScNotches.class);
        base.setTag(ScGauge.BASE_IDENTIFIER);
        base.setPosition(ScFeature.Positions.INSIDE);
        base.setRepetitions(30);
        base.setWidths(width, width * 0.6f, width * 0.8f);
        base.setHeights(height * 0.2f, height);
        base.setColors(Objects.requireNonNull(getContext()).getColor(R.color.backgroundText));
        base = (ScNotches) powerGauge2.addFeature(ScNotches.class);
        base.setTag(ScGauge.BASE_IDENTIFIER);
        base.setPosition(ScFeature.Positions.INSIDE);
        base.setRepetitions(30);
        base.setWidths(width, width * 0.6f, width * 0.8f);
        base.setHeights(height * 0.2f, height);
        base.setColors(getContext().getColor(R.color.backgroundText));

        // Create the progress notches.
        ScNotches progress = (ScNotches) powerGauge.addFeature(ScNotches.class);
        progress.setTag(ScGauge.PROGRESS_IDENTIFIER);
        progress.setColorsMode(ScFeature.ColorsMode.SOLID);
        progress.setColors(
                getContext().getColor(R.color.green),
                getContext().getColor(R.color.yellow),
                getContext().getColor(R.color.red)
        );
        progress = (ScNotches) powerGauge2.addFeature(ScNotches.class);
        progress.setTag(ScGauge.PROGRESS_IDENTIFIER);
        progress.setColorsMode(ScFeature.ColorsMode.SOLID);
        progress.setColors(
                getContext().getColor(R.color.green),
                getContext().getColor(R.color.yellow),
                getContext().getColor(R.color.red)
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
        base.setColors(Objects.requireNonNull(getContext()).getColor(R.color.backgroundText));

        // Create the progress notches.
        ScNotches progress = (ScNotches) batteryGauge.addFeature(ScNotches.class);
        progress.setTag(ScGauge.PROGRESS_IDENTIFIER);
        progress.setColors(
                getContext().getColor(R.color.red),
                getContext().getColor(R.color.red),
                getContext().getColor(R.color.yellow),
                getContext().getColor(R.color.yellow),
                getContext().getColor(R.color.green),
                getContext().getColor(R.color.green),
                getContext().getColor(R.color.green),
                getContext().getColor(R.color.green)

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
        base.setColors(Objects.requireNonNull(getContext()).getColor(R.color.backgroundText));

        // Create the progress notches.
        ScNotches progress = (ScNotches) BMSChargeGauge.addFeature(ScNotches.class);
        progress.setTag(ScGauge.PROGRESS_IDENTIFIER);
        progress.setColors(
                getContext().getColor(R.color.foregroundText),
                getContext().getColor(R.color.blue)
        );

        // Set the value
        BMSChargeGauge.setHighValue(0, 0, 100); // TODO: set proper BMS Charge range
    }

    // Updates field info
    public void setFaultLight(boolean state) {
        if (state) {
//            FaultLight.setChecked(true);
            FaultLight.setTextColor(WHITE);
            FaultLight.setButtonTintList(RED);
            FaultLight.setVisibility(View.VISIBLE);
        } else {
//            FaultLight.setChecked(false);
            FaultLight.setVisibility(View.GONE);
            FaultLight.setTextColor(BG);
            FaultLight.setButtonTintList(BG);
        }
    }

    public void setWaitingLight(boolean state) {
        if (state) {
//            waitingLight.setChecked(true);
            waitingLight.setVisibility(View.VISIBLE);
            waitingLight.setTextColor(WHITE);
            waitingLight.setButtonTintList(YELLOW);
        } else {
//            waitingLight.setChecked(false);
            waitingLight.setVisibility(View.GONE);
            waitingLight.setTextColor(BG);
            waitingLight.setButtonTintList(BG);
        }
    }

    public void setChargingLight(boolean state) {
        if (state) {
//            chargingLight.setChecked(true);
            chargingLight.setVisibility(View.VISIBLE);
            chargingLight.setTextColor(WHITE);
            chargingLight.setButtonTintList(GREEN);
        } else {
//            chargingLight.setChecked(false);
            chargingLight.setVisibility(View.GONE);
            chargingLight.setTextColor(BG);
            chargingLight.setButtonTintList(BG);
        }
    }

    public void setLagLight(boolean state) {
        setLagLight(state, 0);
    }

    public void setLagLight(boolean state, long time) {
        if (state) {
//            lagLight.setChecked(true);
            lagLight.setTextColor(WHITE);
            lagLight.setButtonTintList(YELLOW);
            lagLight.setVisibility(View.VISIBLE);
            if (time == 0)
                lagLightText.setText("");
            else
                lagLightText.setText(String.format(Locale.US,"%dms", time));
        } else {
//            lagLight.setChecked(false);
            lagLight.setTextColor(BG);
            lagLight.setButtonTintList(BG);
            lagLight.setVisibility(View.GONE);
            lagLightText.setText("");
        }
    }

    public void setStartLight(boolean state) {
        if (state) {
//            startLight.setChecked(true);
            startLight.setButtonTintList(GREEN);
        } else {
//            startLight.setChecked(false);
            startLight.setButtonTintList(BG);
        }
    }

    public void setSpeedGauge(long battery) {
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
        BMSChargeValue.setText(String.valueOf(power).concat(" W"));
        BMSChargeGauge.setHighValue(Math.abs(power));
    }

    public void setSpeedometer(long speed) {
        speedometer.setText(String.valueOf(speed));
    }

    public void setCurrentState(String state) {
        currentState.setText(state);
    }

    // TODO: Check if raw battery value received from Teensy is okay
    private static int convertBatteryLife(long battery) {
        //Assumes battery is voltage and max is 302.4V min is 216V
        return (int) ((float) ((battery - 216) / 86.4 * 100.0));
    }
}
