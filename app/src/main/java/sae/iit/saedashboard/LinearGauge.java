package sae.iit.saedashboard;

import android.content.Context;
import android.graphics.Path;
import android.util.AttributeSet;

import androidx.annotation.FloatRange;

import com.sccomponents.gauges.library.ScFeature;
import com.sccomponents.gauges.library.ScGauge;
import com.sccomponents.gauges.library.ScNotches;

public class LinearGauge extends ScGauge {

    private float oldVal = 0;

    public LinearGauge(Context context) {
        super(context);
    }

    public LinearGauge(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public LinearGauge(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    private float DV(float x) {
        return (float) Math.max((0.5 - (Math.pow(x, 2)) / 8), 0.01f);
    }

    @Override
    public void setHighValue(float percentage) {
        percentage = Math.min(percentage, 100f);
        oldVal += (percentage - oldVal) * DV(percentage / 100);
        super.setHighValue(oldVal);
    }

    @Override
    protected Path createPath(int width, int height) {
        Path path = new Path();
        path.lineTo(100, 0);
        return path;
    }

}
