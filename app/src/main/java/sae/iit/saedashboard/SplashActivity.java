package sae.iit.saedashboard;

import android.app.Activity;
import android.app.ActivityOptions;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcelable;
import android.transition.Fade;
import android.transition.Slide;
import android.transition.Transition;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.VideoView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.ActivityOptionsCompat;

public class SplashActivity extends Activity {
    VideoView videoView;
//    TextView colorBlock;

    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            getWindow().getAttributes().layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
        }

        requestWindowFeature(Window.FEATURE_ACTIVITY_TRANSITIONS);
        Fade f = new Fade();
        getWindow().setEnterTransition(f);
        getWindow().setExitTransition(f);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_splash);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

        videoView = findViewById(R.id.videoView);
//        colorBlock = findViewById(R.id.colorBlock);

        View decorView = getWindow().getDecorView();
        int uiOptions = View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
        decorView.setSystemUiVisibility(uiOptions);

        Uri video = Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.startup);
        videoView.setVideoURI(video);
        videoView.setOnCompletionListener(mp -> startNextActivity());
        videoView.start();
//        videoView.postDelayed(() -> colorBlock.setVisibility(View.GONE), 500);
//        videoView.postDelayed(() -> colorBlock.setVisibility(View.VISIBLE), 1650);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        videoView.stopPlayback();
        startNextActivity();
        return super.dispatchTouchEvent(ev);
    }

    private void startNextActivity() {
        if (isFinishing())
            return;
        startActivity(new Intent(this, MainActivity.class), ActivityOptions.makeSceneTransitionAnimation(this).toBundle());
//        finish();
    }
}