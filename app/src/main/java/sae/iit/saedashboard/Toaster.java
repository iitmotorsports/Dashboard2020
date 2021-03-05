package sae.iit.saedashboard;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.Gravity;
import android.widget.Toast;

public class Toaster {
    static Toast wheat;
    @SuppressLint("StaticFieldLeak")
    static Context main;

    @SuppressLint("ShowToast")
    public static void setContext(Context mainContext) {
        main = mainContext;
        wheat = Toast.makeText(mainContext, "nil", Toast.LENGTH_SHORT);
    }

    public static void showToast(String msg) {
        showToast(msg, false, false);
    }

    public static void showToast(String msg, boolean longLength) {
        showToast(msg, longLength, false);
    }

    @SuppressLint("ShowToast")
    public static void showToast(String msg, boolean longLength, boolean rightSide) { // Improve: always reuse same Toast object
        if (wheat == null)
            return;
        if (wheat.getView().isShown()) {
            wheat.getView().setAlpha(0);
            wheat.cancel();
            wheat = Toast.makeText(main, msg, longLength ? Toast.LENGTH_LONG : Toast.LENGTH_SHORT);
        } else {
            wheat.setDuration(longLength ? Toast.LENGTH_LONG : Toast.LENGTH_SHORT);
            wheat.setText(msg);
        }
        wheat.setGravity(Gravity.BOTTOM | (rightSide ? Gravity.END : Gravity.CENTER), 0, 32);
        wheat.show();
    }

}
