package sae.iit.saedashboard;

import android.app.Activity;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.text.SpannableStringBuilder;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.ToggleButton;

import androidx.fragment.app.Fragment;

import java.util.Hashtable;
import java.util.Locale;
import java.util.Objects;

public class PinoutTab extends Fragment {

    private Mirror mirror;
    private RadioButton enableLight;
    private ToggleButton toggleBtn;
    private final Hashtable<Integer, Pin> viewMap = new Hashtable<>();
    private ColorStateList BG, RED, YELLOW, GREEN, WHITE;
    private int BGi, REDi, YELLOWi, GREENi, WHITEi;
    private LinearLayout pinLayout;
    private Activity activity;
    private int width;
    private int height;
    private HorizontalScrollView pinScroller;
    private int setValue = 0;
    private Pin curr;
    private EditText valText;
    LinearLayout currentColumn;
    private TeensyStream stream;

    private class Pin {
        public int pin;
        TextView view;
        Long val = null;
        boolean nil = false;
        boolean set = false;

        Pin(int pin) {
            this.pin = pin;
            view = new TextView(getContext());
            view.setPadding(5, 5, 5, 5);
            view.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f);
            view.setOnClickListener(v -> {
                if (curr != null)
                    curr.view.setBackgroundColor(Color.TRANSPARENT);
                view.setBackgroundColor(Color.WHITE);
                curr = this;
            });
            addView(view);
        }

        int getColor(Long value) {
            if (set) {
                return REDi;
            } else if (value == null || val == null || nil) {
                return BGi;
            } else if (value.equals(val)) {
                return YELLOWi;
            } else {
                return WHITEi;
            }
        }

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.pinout_tab, container, false);
        REDi = Objects.requireNonNull(getContext()).getColor(R.color.red);
        RED = ColorStateList.valueOf(REDi);
        YELLOWi = getContext().getColor(R.color.yellow);
        YELLOW = ColorStateList.valueOf(YELLOWi);
        GREENi = getContext().getColor(R.color.green);
        GREEN = ColorStateList.valueOf(GREENi);
        BGi = getContext().getColor(R.color.backgroundText);
        BG = ColorStateList.valueOf(BGi);
        WHITEi = getContext().getColor(R.color.foregroundText);
        WHITE = ColorStateList.valueOf(WHITEi);

        pinLayout = rootView.findViewById(R.id.PinLayout);
        toggleBtn = rootView.findViewById(R.id.toggleBtn);
        pinScroller = rootView.findViewById(R.id.pinScroller);
        Button clearBtn = rootView.findViewById(R.id.clearBtn);
        Button setBtn = rootView.findViewById(R.id.setBtn);
        valText = rootView.findViewById(R.id.valText);
        ToggleButton updateBtn = rootView.findViewById(R.id.updateBtn);
        enableLight = rootView.findViewById(R.id.enableLight);

        valText.setOnEditorActionListener((v, actionId, event) -> {
            try {
                setValue = Integer.parseInt(String.valueOf(((EditText) v).getText()));
            } catch (NumberFormatException e) {
                setBtn.setEnabled(false);
                return false;
            }
            setBtn.setEnabled(true);
            return false;
        });
        setBtn.setOnClickListener(v -> {
            if (curr != null) {
                mirror.setPin(curr.pin, setValue);
            }
        });
        toggleBtn.setOnClickListener(v -> {
            toggleBtn.setChecked(false);
            mirror.toggle();
        });
        clearBtn.setOnClickListener(v -> {
            curr = null;
            currentColumn = null;
            activity.runOnUiThread(() -> pinLayout.removeAllViewsInLayout());
            viewMap.clear();
            setStream(stream, activity);
        });
        updateBtn.setOnClickListener(v -> mirror.enableUpdate(updateBtn.isChecked()));

        DisplayMetrics display = this.getResources().getDisplayMetrics();
        this.width = display.widthPixels;
        this.height = display.heightPixels;

        return rootView;
    }

    private void onEnable() {
        enableLight.setButtonTintList(GREEN);
        toggleBtn.setChecked(true);
    }

    private void onDisable() {
        enableLight.setButtonTintList(BG);
        toggleBtn.setChecked(false);
    }

    private void addView(TextView view) {
        activity.runOnUiThread(() -> {
            if (currentColumn == null || (currentColumn.getHeight() + 42) > pinScroller.getHeight() - 10) {
                currentColumn = new LinearLayout(getContext());
                currentColumn.setOrientation(LinearLayout.VERTICAL);
                currentColumn.setPadding(5, 5, 5, 5);
                pinLayout.addView(currentColumn);
            }
            currentColumn.addView(view);
        });
    }

    private void updateValue(int pin, Long value) {
        Pin pinClass = viewMap.get(pin);
        if (pinClass == null) {
            pinClass = new Pin(pin);
            viewMap.put(pin, pinClass);
        }
        TextView finalTextView = pinClass.view;
        SpannableStringBuilder val = new SpannableStringBuilder();
        val.append(TeensyStream.getColoredString(String.format(Locale.US, "%1$3s : ", pin), BGi));
        val.append(TeensyStream.getColoredString(String.valueOf(value), pinClass.getColor(value)));
        activity.runOnUiThread(() -> finalTextView.setText(val));
        pinClass.val = value;
//        pinClass.set = false;
    }

    private void onFail(int pin) {
        Pin pinClass = viewMap.get(pin);
        if (pinClass != null) {
            pinClass.nil = true;
            if (pinClass.set) {
                pinClass.set = false;
            }
            updateValue(pin, 0L);
        }
    }

    private void onSet(int pin, long value) {
        Pin pinClass = viewMap.get(pin);
        if (pinClass != null) {
            pinClass.nil = false;
            pinClass.set = true;
        }
        updateValue(pin, value);
    }

    public void setStream(TeensyStream stream, Activity activity) {
        this.stream = stream;
        this.activity = activity;
        if (this.mirror != null)
            this.mirror.disable();
        this.mirror = new Mirror(stream, this::onEnable, this::onDisable, this::updateValue, this::onFail, this::onSet);
    }

}