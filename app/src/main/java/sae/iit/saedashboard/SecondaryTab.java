package sae.iit.saedashboard;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

public class SecondaryTab extends Fragment {
    private TextView rightMotorTempValue, leftMotorTempValue, rightMCTempValue, leftMCTempValue, activeAeroPosValue, DCBusCurrentValue;
    private LinearGauge leftMCTempGauge, leftMotorTempGauge, rightMCTempGauge, rightMotorTempGauge, DCBusCurrentGauge, activeAeroPosGauge;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        ViewGroup rootView = (ViewGroup) inflater.inflate(
                R.layout.secondary_tab, container, false);

        //Initializing Fields
        rightMotorTempValue = rootView.findViewById(R.id.rightMotorTempValue);
        leftMotorTempValue = rootView.findViewById(R.id.leftMotorTempValue);
        rightMCTempValue = rootView.findViewById(R.id.rightMCTempValue);
        leftMCTempValue = rootView.findViewById(R.id.leftMCTempValue);
        activeAeroPosValue = rootView.findViewById(R.id.activeAeroPosValue);
        DCBusCurrentValue = rootView.findViewById(R.id.DCBusCurrentValue);

        int RED = Color.parseColor("#EA0C01");
        int YELLOW = Color.parseColor("#FEF301");
        int GREEN = Color.parseColor("#0BA60A");
        int BG = Color.parseColor("#3A3D4F");

        // TODO: set proper ranges/colors
        leftMCTempGauge = rootView.findViewById(R.id.leftMCTempGauge);
        leftMCTempGauge.initialize(0, 0, 100, 0.8f, 50, 5, 250, BG, GREEN, YELLOW, RED);
        leftMotorTempGauge = rootView.findViewById(R.id.leftMotorTempGauge);
        leftMotorTempGauge.initialize(0, 0, 100, 0.8f, 50, 5, 250, BG, GREEN, YELLOW, RED);
        rightMCTempGauge = rootView.findViewById(R.id.rightMCTempGauge);
        rightMCTempGauge.initialize(0, 0, 100, 0.8f, 50, 5, 250, BG, GREEN, YELLOW, RED);
        rightMotorTempGauge = rootView.findViewById(R.id.rightMotorTempGauge);
        rightMotorTempGauge.initialize(0, 0, 100, 0.8f, 50, 5, 250, BG, GREEN, YELLOW, RED);
        DCBusCurrentGauge = rootView.findViewById(R.id.DCBusCurrentGauge);
        DCBusCurrentGauge.initialize(0, 0, 100, 0.8f, 50, 5, 250, BG, GREEN, YELLOW, RED);
        activeAeroPosGauge = rootView.findViewById(R.id.activeAeroPosGauge);
        activeAeroPosGauge.initialize(0, 0, 100, 0.8f, 50, 5, 250, BG, GREEN, YELLOW, RED);

        return rootView;
    }

    public void setValues(long leftMCTemp, long leftMotorTemp, long rightMCTemp, long rightMotorTemp, long DCBus, long activeAero) {
        leftMCTempGauge.setHighValue((float) leftMCTemp);
        leftMotorTempGauge.setHighValue((float) leftMotorTemp);
        rightMCTempGauge.setHighValue((float) rightMCTemp);
        rightMotorTempGauge.setHighValue((float) rightMotorTemp);
        DCBusCurrentGauge.setHighValue((float) DCBus);
        activeAeroPosGauge.setHighValue((float) activeAero);

        leftMCTempValue.setText(String.valueOf(leftMCTemp));
        leftMotorTempValue.setText(String.valueOf(leftMotorTemp));
        rightMotorTempValue.setText(String.valueOf(rightMCTemp));
        rightMCTempValue.setText(String.valueOf(rightMotorTemp));
        DCBusCurrentValue.setText(String.valueOf(DCBus));
        activeAeroPosValue.setText(String.valueOf(activeAero));
    }

}