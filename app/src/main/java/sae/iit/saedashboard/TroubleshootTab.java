package sae.iit.saedashboard;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.fragment.app.Fragment;

import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;

public class TroubleshootTab extends Fragment implements SideControlSize {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.troubleshoot_tab, container, false);
        final ImageView image = rootView.findViewById(R.id.carImage);
        TimerTask rgb = new TimerTask() {
            float rgb = -180;

            @Override
            public void run() {
                rgb += 0.1;
                if (rgb > 180)
                    rgb = -180;
                Objects.requireNonNull(getActivity()).runOnUiThread(() -> image.setColorFilter(ColorFilterGenerator.adjustHue(rgb)));
            }
        };
        Timer rgbTimer = new Timer();
        rgbTimer.scheduleAtFixedRate(rgb, 1000, 10);
        return rootView;
    }

    @Override
    public int getPanelSize() {
        return 0;
    }
}