package sae.iit.saedashboard;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

public class SecondaryTab extends Fragment {
    private TextView noneval, BMSCurr, batTemp, bmsVolt, dischargeLimit, chargeLimit;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.secondary_tab, container, false);
        //Initializing Fields
        bmsVolt = rootView.findViewById(R.id.Value0);
        batTemp = rootView.findViewById(R.id.Value1);
        chargeLimit = rootView.findViewById(R.id.Value2);
        BMSCurr = rootView.findViewById(R.id.Value3);
        noneval = rootView.findViewById(R.id.Value4);
        dischargeLimit = rootView.findViewById(R.id.Value5);

        return rootView;
    }

    public void setValues(long bmsVolt, long batTemp, long chargeLimit, long BMSCurr, long dischargeLimit) {
        this.bmsVolt.setText(String.valueOf(bmsVolt));
        this.batTemp.setText(String.valueOf(batTemp));
        this.chargeLimit.setText(String.valueOf(chargeLimit));
        this.BMSCurr.setText(String.valueOf(BMSCurr));
//        this.noneval.setText(String.valueOf(noneval));
        this.dischargeLimit.setText(String.valueOf(dischargeLimit));
    }

}