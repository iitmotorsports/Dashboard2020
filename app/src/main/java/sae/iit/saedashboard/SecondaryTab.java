package sae.iit.saedashboard;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

public class SecondaryTab extends Fragment {
    private TextView rightMotorTempValue, leftMotorTempValue, rightMCTempValue, leftMCTempValue, activeAeroPosValue, DCBusCurrentValue;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.secondary_tab, container, false);
        //Initializing Fields
        rightMotorTempValue = rootView.findViewById(R.id.rightMotorTempValue);
        leftMotorTempValue = rootView.findViewById(R.id.leftMotorTempValue);
        rightMCTempValue = rootView.findViewById(R.id.rightMCTempValue);
        leftMCTempValue = rootView.findViewById(R.id.leftMCTempValue);
        activeAeroPosValue = rootView.findViewById(R.id.activeAeroPosValue);
        DCBusCurrentValue = rootView.findViewById(R.id.DCBusCurrentValue);
        return rootView;
    }

    public void setValues(long leftMCTemp, long leftMotorTemp, long rightMCTemp, long rightMotorTemp, long DCBus, long activeAero) {
        leftMCTempValue.setText(String.valueOf(leftMCTemp));
        leftMotorTempValue.setText(String.valueOf(leftMotorTemp));
        rightMotorTempValue.setText(String.valueOf(rightMCTemp));
        rightMCTempValue.setText(String.valueOf(rightMotorTemp));
        DCBusCurrentValue.setText(String.valueOf(DCBus));
        activeAeroPosValue.setText(String.valueOf(activeAero));
    }

}