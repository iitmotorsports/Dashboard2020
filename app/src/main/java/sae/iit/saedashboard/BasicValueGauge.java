package sae.iit.saedashboard;

import android.app.Activity;
import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;

class BasicValueGaugeView extends RelativeLayout {

    public BasicValueGaugeView(Context context) {
        this(context, null);
    }

    public BasicValueGaugeView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    private final TextView number, info;

    public BasicValueGaugeView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        View headerView = LayoutInflater.from(context).inflate(R.layout.basic_value_gauge_layout, this, true);
        number = findViewById(R.id.GaugeValue);
        info = findViewById(R.id.GaugeText);
        this.setLayoutParams(new ConstraintLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.BasicValueGaugeView);
        final int N = a.getIndexCount();
        for (int i = 0; i < N; ++i) {
            int attr = a.getIndex(i);
            if (attr == R.styleable.BasicValueGaugeView_Info) {
                info.setText(a.getString(attr));
            } else if (attr == R.styleable.BasicValueGaugeView_Value) {
                number.setText(String.valueOf(a.getInteger(attr, 0)));
            }
        }
        a.recycle();
    }

    public void setValue(Number value) {
        this.number.setText(value.toString());
    }

    public TextView getValueView() {
        return number;
    }

    public void setText(String value) {
        this.info.setText(value);
    }
}


