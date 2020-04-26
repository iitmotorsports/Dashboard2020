package sae.iit.saedashboard;
import android.app.Activity;
import android.content.Intent;
import android.os.Handler;
import android.os.Bundle;

public class SplashActivity extends Activity {
	Handler handler;
	@Override
	protected void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_splash);

		handler=new Handler();
		handler.postDelayed(new Runnable() {
			@Override
				 public void run() {
				Intent intent = new Intent(SplashActivity.this,MainActivity.class);
				startActivity(intent);
				finish();
				}
			},3000);
		}
	}
