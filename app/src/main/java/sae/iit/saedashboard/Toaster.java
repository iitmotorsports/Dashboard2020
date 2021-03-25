package sae.iit.saedashboard;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.view.Gravity;
import android.widget.Toast;

@SuppressLint("StaticFieldLeak")
public class Toaster {
    static Toast wheat;
    static Context main;
    static Activity act;

    @SuppressLint("ShowToast")
    public static void setContext(Activity activity) {
        main = activity.getBaseContext();
        act = activity;
        wheat = Toast.makeText(main, "nil", Toast.LENGTH_SHORT);
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
            act.runOnUiThread(() -> {
                wheat.getView().setAlpha(0);
                wheat.cancel();
                wheat = Toast.makeText(main, msg, longLength ? Toast.LENGTH_LONG : Toast.LENGTH_SHORT);
            });
        } else {
            wheat.setDuration(longLength ? Toast.LENGTH_LONG : Toast.LENGTH_SHORT);
            act.runOnUiThread(() -> wheat.setText(msg));
        }
        wheat.setGravity(Gravity.BOTTOM | (rightSide ? Gravity.END : Gravity.CENTER), 0, 32);
        act.runOnUiThread(() -> wheat.show());
        Log.i("Toaster", msg);
    }

}
