package sae.iit.saedashboard;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager.widget.ViewPager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.felhr.usbserial.UsbSerialDevice;
import com.felhr.usbserial.UsbSerialInterface;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import com.google.android.material.tabs.TabLayout;

public class MainActivity extends AppCompatActivity {

	//Initializing variable
	public String s = "0";
	public String pg = "0";
	public String bl = "0";
	public String lmt = "0";
	public String rmt = "0";
	public String lmct = "0";
	public String rmct = "0";
	public String aap = "0";
	public String soc = "0";
	public String DCbc = "0";
	public String f = "0";
	public String ttt = "0";
	public int speed = 0;
	public String v = "50";
	public int volt = 50;
	public String b = "blank";
	public boolean be = false;
	public String[] values = new String[13];
	public final String ACTION_USB_PERMISSION = "com.hariharan.arduinousb.USB_PERMISSION";
	static public int[] List;
	private static final int NUM_OF_TABS = 4;
	private ViewPager viewPager;
	private MyPagerAdapter pagerAdapter;
	private TabLayout tabLayout;

	private MainTab mainTab = (MainTab) getSupportFragmentManager().findFragmentById(R.id.mainTab);
	private SecondaryTab secondaryTab = (SecondaryTab) getSupportFragmentManager().findFragmentById(R.id.secondaryTab);
	private TroubleshootTab troubleshootTab = (TroubleshootTab) getSupportFragmentManager().findFragmentById(R.id.troubleshootTab);
	private ListView lv;
	private String line;
	Button startButton, closeButton, clearButton;
	TextView sconsole;
	UsbManager usbManager;
	UsbDevice device;
	UsbSerialDevice serialPort;
	UsbDeviceConnection connection;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		//Hides the status bar.
		View decorView = getWindow().getDecorView();
		int uiOptions = View.SYSTEM_UI_FLAG_FULLSCREEN;
		decorView.setSystemUiVisibility(uiOptions);
		//Setting up ViewPager, Adapter, and TabLayout
		viewPager = findViewById(R.id.viewPager);
		pagerAdapter = new MyPagerAdapter(getSupportFragmentManager());
		tabLayout = findViewById(R.id.tabLayout);
		viewPager.setAdapter(pagerAdapter);
		tabLayout.setupWithViewPager(viewPager);
		tabLayout.setTabTextColors(getResources().getColor(R.color.white),getResources().getColor(R.color.white));
		startButton = findViewById(R.id.Start);
		closeButton = findViewById(R.id.Close);
		clearButton = findViewById(R.id.clearButton);
		sconsole = findViewById(R.id.textView7);
		//setUiEnabled(false);
		usbManager = (UsbManager) getSystemService(USB_SERVICE);
		IntentFilter filter = new IntentFilter();
		filter.addAction(ACTION_USB_PERMISSION);
		filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
		filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
		registerReceiver(broadcastReceiver, filter);
		tvAppend(sconsole,"This is a test");

		//Background update Function. MOST IMPORTANT
		Timer timer=new Timer();
		timer.scheduleAtFixedRate(new TimerTask(){
			@Override
			public void run(){
				try{
					for(int i =0;i<10;i++){
						values[i] = line.split(" ")[i];
					}
					s = values[1];
					pg = values[2];
					bl = values[3];
					lmt = values[4];
					rmt = values[5];
					lmct = values[6];
					rmct = values[7];
					aap = values[8];
					DCbc = values[9];
					//soc = values[10];
					//f = values[11];
					//ttt = values[12];
				} catch(NullPointerException e){}
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						try {

							MainTab.setSpeedometer(s);
							MainTab.setPowerGauge(pg);
							MainTab.setBatteryLife(bl);
							MainTab.setBatImage(bl);
							SecondaryTab.setLeftMotorTemp(lmt);
							SecondaryTab.setRightMotorTemp(rmt);
							SecondaryTab.setLeftMotorContTemp(lmct);
							SecondaryTab.setRightMotorContTemp(rmct);
							SecondaryTab.setActiveAeroPos(aap);
							SecondaryTab.setDCBusCurrent(DCbc);

						}catch(NullPointerException e){}
					}
				});
			}
		},0,50);

	}
	public void onClickStart(View view){
		HashMap<String, UsbDevice> usbDevices = usbManager.getDeviceList();
		if(!usbDevices.isEmpty()){
			boolean keep =true;
			for(Map.Entry<String,UsbDevice> entry : usbDevices.entrySet()){
				device = entry.getValue();
				int deviceVID = device.getVendorId();
				if(deviceVID == 5824){
					PendingIntent pi = PendingIntent.getBroadcast(this,0,new Intent(ACTION_USB_PERMISSION),0);
					usbManager.requestPermission(device,pi);
					keep = false;
				} else {
					tvAppend(sconsole,"\n"+deviceVID+"\n");
					connection = null;
					device = null;
				}

				if(!keep)
					break;
			}
		}
	}
	public void onClickStop(View view){
		setUiEnabled(false);
		serialPort.close();
		tvAppend(sconsole,"\nSerial Connection Closed!");
	}
	private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			if(intent.getAction().equals(ACTION_USB_PERMISSION)){
				tvAppend(sconsole,"We Good 1");
				boolean granted = intent.getExtras().getBoolean(UsbManager.EXTRA_PERMISSION_GRANTED);
				if(granted){
					tvAppend(sconsole,"We Good 2");
					connection = usbManager.openDevice(device);
					serialPort = UsbSerialDevice.createUsbSerialDevice(device,connection);
					if(serialPort != null){
						tvAppend(sconsole,"We Good 3");
						if(serialPort.open()){
							setUiEnabled(true);
							serialPort.setBaudRate(9600);
							serialPort.setDataBits(UsbSerialInterface.DATA_BITS_8);
							serialPort.setStopBits(UsbSerialInterface.STOP_BITS_1);
							serialPort.setParity(UsbSerialInterface.PARITY_NONE);
							serialPort.setFlowControl(UsbSerialInterface.FLOW_CONTROL_OFF);
							serialPort.read(mCallback);
							tvAppend(sconsole,"Serial Connection Opened!\n");
						} else{
							tvAppend(sconsole,"Not good PORT");
						}
					} else {
						tvAppend(sconsole,"Not good no serial");
					}
				} else {
					tvAppend(sconsole,"not good at all");
				}
			} else if(intent.getAction().equals(UsbManager.ACTION_USB_DEVICE_ATTACHED)){
				tvAppend(sconsole,"connected");
				onClickStart(startButton);
			} else if (intent.getAction().equals(UsbManager.ACTION_USB_DEVICE_DETACHED)){
				onClickStop(closeButton);
			}
		}
	};
	private void tvAppend(TextView tv,CharSequence text){
		final TextView ftv = tv;
		final CharSequence ftext = text;
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				ftv.append(ftext);
			}
		});
	}
	public void setUiEnabled(boolean bool){
		startButton.setEnabled(!bool);
		closeButton.setEnabled(bool);
	}
	public void onClickFault(){
		serialPort.write("0000".getBytes());
	}
	public void onClickClear(View view){
		sconsole.setText(" ");
	}
	UsbSerialInterface.UsbReadCallback mCallback = new UsbSerialInterface.UsbReadCallback() {
		@Override
		public void onReceivedData(byte[] arg0){
			String data;

			if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT){
				data = new String(arg0,StandardCharsets.UTF_8);
			} else {
				data = new String(arg0, Charset.defaultCharset());
			}
			line = data;
			data.concat("/n");
			tvAppend(sconsole,line+"/n");
		}
	};
	/*protected void onResume(Bundle savedInstanceState) {
		//Hides the status bar.
		View decorView = getWindow().getDecorView();
		int uiOptions = View.SYSTEM_UI_FLAG_FULLSCREEN;
		decorView.setSystemUiVisibility(uiOptions);
	}*/

	//Updates display on tablet
	//Will receive an array
	static public void update() { }

	@Override
	public void onDestroy(){
		unregisterReceiver(broadcastReceiver);
		super.onDestroy();
	}
}
