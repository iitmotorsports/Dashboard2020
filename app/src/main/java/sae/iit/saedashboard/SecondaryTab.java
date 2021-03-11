package sae.iit.saedashboard;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

public class SecondaryTab extends Fragment {
    private TextView rightMotorTemp, leftMotorTemp, rightMotorContTemp, leftMotorContTemp, activeAeroPos, DCBusCurrent;
    private LinearGauge leftMCTempGauge, leftMotorTempGauge, rightMCTempGauge, rightMotorTempGauge, DCBusCurrentGauge, activeAeroPosGauge;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        ViewGroup rootView = (ViewGroup) inflater.inflate(
                R.layout.secondary_tab, container, false);

        //Initializing Fields
        rightMotorTemp = rootView.findViewById(R.id.rightMotorTemp);
        leftMotorTemp = rootView.findViewById(R.id.leftMotorTemp);
        rightMotorContTemp = rootView.findViewById(R.id.rightMotorContTemp);
        leftMotorContTemp = rootView.findViewById(R.id.leftMotorContTemp);
        activeAeroPos = rootView.findViewById(R.id.activeAeroPos);
        DCBusCurrent = rootView.findViewById(R.id.DCBusCurrent);

        int RED = Color.parseColor("#EA0C01");
        int YELLOW = Color.parseColor("#FEF301");
        int GREEN = Color.parseColor("#0BA60A");
        int BG = Color.parseColor("#3A3D4F");

        // TODO: set proper ranges/colors
        leftMCTempGauge = rootView.findViewById(R.id.leftMCTempGauge);
        leftMCTempGauge.initialize(0, 0, 100, 0.8f, 100, 5, 250, BG, GREEN, YELLOW, RED);
        leftMotorTempGauge = rootView.findViewById(R.id.leftMotorTempGauge);
        leftMotorTempGauge.initialize(0, 0, 100, 0.8f, 100, 5, 250, BG, GREEN, YELLOW, RED);
        rightMCTempGauge = rootView.findViewById(R.id.rightMCTempGauge);
        rightMCTempGauge.initialize(0, 0, 100, 0.8f, 100, 5, 250, BG, GREEN, YELLOW, RED);
        rightMotorTempGauge = rootView.findViewById(R.id.rightMotorTempGauge);
        rightMotorTempGauge.initialize(0, 0, 100, 0.8f, 100, 5, 250, BG, GREEN, YELLOW, RED);
        DCBusCurrentGauge = rootView.findViewById(R.id.DCBusCurrentGauge);
        DCBusCurrentGauge.initialize(0, 0, 100, 0.8f, 100, 5, 250, BG, GREEN, YELLOW, RED);
        activeAeroPosGauge = rootView.findViewById(R.id.activeAeroPosGauge);
        activeAeroPosGauge.initialize(0, 0, 100, 0.8f, 100, 5, 250, BG, GREEN, YELLOW, RED);

        return rootView;
    }

    public void setLeftMotorTemp(String LMT) {
        leftMotorTemp.setText(LMT);
    }

    public void setRightMotorTemp(String RMT) {
        rightMotorTemp.setText(RMT);
    }

    public void setLeftMotorContTemp(String LMCT) {
        leftMotorContTemp.setText(LMCT);
    }

    public void setRightMotorContTemp(String RMCT) {
        rightMotorContTemp.setText(RMCT);
    }

    public void setActiveAeroPos(String AAP) {
        activeAeroPos.setText(AAP);
    }

    public void setDCBusCurrent(String DCBC) {
        DCBusCurrent.setText(DCBC);
    }

}