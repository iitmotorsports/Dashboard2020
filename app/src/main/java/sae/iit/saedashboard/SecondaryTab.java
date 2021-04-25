package sae.iit.saedashboard;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class SecondaryTab extends Fragment implements SideControlSize {
    private final String[] SET_VALUES = {
            "MC0 DC BUS Volt",
            "MC0 DC BUS Curr",
            "MC1 DC BUS Volt",
            "MC1 DC BUS Curr",
            "BMS Voltage",
            "BMS Avg Curr",
            "BMS Highest Temp",
            "BMS Lowest Temp",
            "Discharge curr limit",
            "Charge curr limit",
    };
    private final List<TextView> values = new ArrayList<>();

    LinearLayout MainLayout, currentColumn;
    int count = 0;

    private void newColumn() {
        LinearLayout newCol = new LinearLayout(getContext());
        newCol.setOrientation(LinearLayout.VERTICAL);
        newCol.setPadding(5, 5, 5, 5);
        count = 0;
        currentColumn = newCol;
        MainLayout.addView(currentColumn);
    }

    private void addValue(String value) {
        BasicValueGaugeView BG = new BasicValueGaugeView(getContext());
        BG.setValue(-1);
        BG.setText(value);
        values.add(BG.getValueView());
        currentColumn.addView(BG);
        if (++count >= 3)
            newColumn();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.secondary_tab, container, false);
        MainLayout = rootView.findViewById(R.id.MainLayout);

        newColumn();
        for (String s : SET_VALUES) {
            addValue(s);
        }

        return rootView;
    }

    public void setValues(long[] values) {
        int i = 0;
        for (TextView tv : this.values) {
            tv.setText(String.valueOf(values[i++]));
        }
    }

    @Override
    public int getPanelSize() {
        return 0;
    }
}