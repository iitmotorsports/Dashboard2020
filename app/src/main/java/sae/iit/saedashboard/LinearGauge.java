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

    public void initialize(float value, float startRange, float endRange, @FloatRange(from = 0.0, to = 1.0) float alpha, int repetitions, float width, float height, int BGColor, int... colors) {
        this.removeAllFeatures();
        this.setAlpha(alpha);

        ScNotches base = (ScNotches) this.addFeature(ScNotches.class);
        base.setTag(ScGauge.BASE_IDENTIFIER);
        base.setPosition(ScFeature.Positions.MIDDLE);
        base.setRepetitions(repetitions);
        base.setWidths(width);
        base.setHeights(height);
        base.snapToNotches(1f);
        base.setColors(BGColor);
        ScNotches progress = (ScNotches) this.addFeature(ScNotches.class);
        progress.setTag(ScGauge.PROGRESS_IDENTIFIER);
        progress.setColors(colors);
        this.setHighValue(value, startRange, endRange);
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
