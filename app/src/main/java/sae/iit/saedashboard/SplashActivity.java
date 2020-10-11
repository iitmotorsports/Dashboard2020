package sae.iit.saedashboard;
import android.app.Activity;
import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Handler;
import android.os.Bundle;
import android.widget.VideoView;

public class SplashActivity extends Activity {
	VideoView videoView;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_splash);
		videoView = findViewById(R.id.videoView);

		Uri video = Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.startup);
		videoView.setVideoURI(video);

		videoView.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
			public void onCompletion(MediaPlayer mp) {
				startNextActivity();
			}
		});

		videoView.start();
	}

	private void startNextActivity() {
		if (isFinishing())
			return;
		startActivity(new Intent(this, MainActivity.class));
		finish();
	}
}

