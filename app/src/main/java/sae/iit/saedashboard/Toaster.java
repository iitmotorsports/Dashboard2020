package sae.iit.saedashboard;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.util.Log;
import android.view.Gravity;
import android.widget.Toast;

import es.dmoral.toasty.Toasty;

@SuppressLint("StaticFieldLeak")
public class Toaster {

    static Activity act;

    private static boolean enable = true;

    public enum STATUS {
        NORMAL,
        INFO,
        SUCCESS,
        WARNING,
        ERROR,
    }

    public static void setContext(Activity activity) {
        act = activity;
        Toasty.Config.getInstance().tintIcon(true).setTextSize(14).allowQueue(false).apply();
    }

    public static void showToast(String msg) {
        showToast(msg, false);
    }

    public static void showToast(String msg, boolean longLength) {
        showToast(msg, longLength, false);
    }

    public static void showToast(String msg, boolean longLength, boolean rightSide) {
        showToast(msg, longLength, rightSide, STATUS.NORMAL);
    }

    public static void showToast(String msg, STATUS status) {
        showToast(msg, false, false, status);
    }

    public static void showToast(String msg, boolean longLength, STATUS status) {
        showToast(msg, longLength, false, status);
    }

    public static void showToast(String msg, boolean longLength, boolean rightSide, STATUS status) { // Improve: always reuse same Toast object
        Log.i("Toast", msg);
        if (!enable)
            return;
        act.runOnUiThread(() -> {
            Toast toast;
            switch (status) {
                case NORMAL:
                    toast = Toasty.normal(act.getBaseContext(), msg, longLength ? Toast.LENGTH_LONG : Toast.LENGTH_SHORT);
                    break;
                case INFO:
                    toast = Toasty.info(act.getBaseContext(), msg, longLength ? Toast.LENGTH_LONG : Toast.LENGTH_SHORT);
                    break;
                case SUCCESS:
                    toast = Toasty.success(act.getBaseContext(), msg, longLength ? Toast.LENGTH_LONG : Toast.LENGTH_SHORT);
                    break;
                case WARNING:
                    toast = Toasty.warning(act.getBaseContext(), msg, longLength ? Toast.LENGTH_LONG : Toast.LENGTH_SHORT);
                    break;
                case ERROR:
                    toast = Toasty.error(act.getBaseContext(), msg, longLength ? Toast.LENGTH_LONG : Toast.LENGTH_SHORT);
                    break;
                default:
                    return;
            }
            toast.setGravity(Gravity.BOTTOM | (rightSide ? Gravity.END : Gravity.CENTER), rightSide ? 16 : 0, 16);
            toast.show();
        });
    }

    public static void setEnabled(boolean enable) {
        Toaster.enable = enable;
    }

}
