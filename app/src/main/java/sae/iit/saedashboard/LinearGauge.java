package sae.iit.saedashboard;

import android.content.Context;
import android.graphics.Path;
import android.util.AttributeSet;

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
    protected void onSizeChanged(int xNew, int yNew, int xOld, int yOld) {
        ScNotches base = (ScNotches) this.findFeature(ScGauge.BASE_IDENTIFIER);
        if (base.getHeights().length == 1) {
            base.setHeights(yNew * 2);
            ScNotches progress = (ScNotches) this.findFeature(ScGauge.PROGRESS_IDENTIFIER);
            progress.setHeights(yNew * 2);
        }
        super.onSizeChanged(xNew, yNew, xOld, yOld);
    }

    @Override
    protected Path createPath(int width, int height) {
        // the new path and the area
        Path path = new Path();

        path.lineTo(100, 0);

        // Return the path
        return path;
    }
}
