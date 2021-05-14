package sae.iit.saedashboard;

import android.content.Context;
import android.graphics.Color;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;

public class BasicBarGauge extends RelativeLayout {

    private final TextView subText, mainText, colorBar, extraText;
    private int maxWidth = 0;
    private int[] colors;
    private float oldVal = 0;
    private boolean flipped = false;
    private boolean colorPhase = false;

    public BasicBarGauge(Context context) {
        this(context, null);
    }

    public BasicBarGauge(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public BasicBarGauge(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        View headerView = LayoutInflater.from(context).inflate(R.layout.basic_bar_layout, this, true);
        subText = findViewById(R.id.subText);
        mainText = findViewById(R.id.mainText);
        extraText = findViewById(R.id.extraText);
        colorBar = findViewById(R.id.colorBar);
        colorBar.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                colorBar.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                maxWidth = colorBar.getWidth() + 1;
            }
        });
    }

    public void setText(String text) {
        mainText.setText(text);
    }

    public void setExtraText(String text) {
        extraText.setText(text);
    }

    public void setSubText(String text) {
        subText.setText(text);
    }

    private float DV(float x) {
        return (float) Math.max((0.5 - (Math.pow(x, 2)) / 8), 0.01f);
    }

    private int getColor(float ratio, boolean isSmooth) {
        if (ratio <= 0 || colors.length == 1)
            return colors[0];
        if (ratio >= 1)
            return colors[colors.length - 1];

        // Smooth value
        if (isSmooth) {
            // Calc the sector
            float position = ((colors.length - 1) * ratio);
            int sector = (int) position;
            ratio = position - sector;

            // Get the color to mix
            int sColor = colors[sector];
            int eColor = colors[sector + 1];

            // Manage the transparent case
            if (sColor == Color.TRANSPARENT)
                sColor = Color.argb(0, Color.red(eColor), Color.green(eColor), Color.blue(eColor));
            if (eColor == Color.TRANSPARENT)
                eColor = Color.argb(0, Color.red(sColor), Color.green(sColor), Color.blue(sColor));

            // Calculate the result color
            int alpha = (int) (Color.alpha(eColor) * ratio + Color.alpha(sColor) * (1 - ratio));
            int red = (int) (Color.red(eColor) * ratio + Color.red(sColor) * (1 - ratio));
            int green = (int) (Color.green(eColor) * ratio + Color.green(sColor) * (1 - ratio));
            int blue = (int) (Color.blue(eColor) * ratio + Color.blue(sColor) * (1 - ratio));

            // Get the color
            return Color.argb(alpha, red, green, blue);

        } else {
            // Rough value
            int sector = (int) (colors.length * ratio);
            return colors[sector];
        }
    }

    public void setColors(int... values) {
        this.colors = values;
        if (this.colors.length > 0)
            colorPhase = true;
    }

    public void setPercentage(float percentage) {
        percentage = Math.max(Math.min(percentage, 100f), 0f) / 100f;
        MarginLayoutParams params = (MarginLayoutParams) colorBar.getLayoutParams();
        float newVal = (int) (maxWidth - (maxWidth * percentage));
        oldVal += (newVal - oldVal) * DV(percentage);
        if (newVal != oldVal) {
            if (flipped)
                params.rightMargin = (int) oldVal;
            else
                params.leftMargin = (int) oldVal;
            colorBar.requestLayout();
            if (colorPhase)
                setColor(getColor(percentage, true));
        }
    }

    public void setColor(int color) {
        colorBar.setBackgroundColor(color);
    }

    public void flip() {
        flipped = !flipped;
    }
}
