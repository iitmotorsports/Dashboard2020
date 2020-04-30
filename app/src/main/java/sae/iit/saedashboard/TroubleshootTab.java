package sae.iit.saedashboard;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.view.ViewGroup.LayoutParams;
import android.widget.TextView;
import androidx.fragment.app.Fragment;
import static android.content.Context.LAYOUT_INFLATER_SERVICE;

/*Designer: Deep Trapasia
SAE Electrical Team Member(Software Team) 2019-2020
 */
//implements View.OnClickListener
public class TroubleshootTab extends Fragment  {

	public Button TSmotorcon2, TSPostRunCurrentSensor, TSPostRunAccelerator, TSPostRunTemperature, TSPostRunVoltageSensor, TSPostRunPreCharge,
			      TSmotorcon1, TSPostRunBrake, TSRunCan, TSRunDisCharge, TSRunBrake, Dismiss;

	public ViewGroup rootView;
	public TextView theScon, fixCan, fixMotorCon1, fixMotorCon2, fixAccelerator;
	private static ImageView gray;
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
							 Bundle savedInstanceState) {
		ViewGroup rootView = (ViewGroup) inflater.inflate(
				R.layout.troubleshoot_tab, container, false);

		TSRunCan = rootView.findViewById(R.id.TSRunCan);
		TSmotorcon1 = rootView.findViewById(R.id.TSmotorcon1);
		TSmotorcon2 = rootView.findViewById(R.id.TSmotorcon2);
		TSPostRunAccelerator = rootView.findViewById(R.id.TSPostRunAccelerator);

		fixCan = rootView.findViewById(R.id.fixCan);
		fixCan.setVisibility(View.INVISIBLE);
		fixMotorCon1 = rootView.findViewById(R.id.fixMotorCon1);
		fixMotorCon1.setVisibility(View.INVISIBLE);
		fixMotorCon2 = rootView.findViewById(R.id.fixMotorCon2);
		fixMotorCon2.setVisibility(View.INVISIBLE);
		fixAccelerator = rootView.findViewById(R.id.fixAccelerator);
		fixAccelerator.setVisibility(View.INVISIBLE);

		gray = rootView.findViewById(R.id.gray);
		gray.setVisibility(View.INVISIBLE);
		Dismiss = rootView.findViewById(R.id.dismiss);
		Dismiss.setVisibility(View.INVISIBLE);


		Dismiss.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				Dismiss.setVisibility(View.INVISIBLE);
				gray.setVisibility(View.INVISIBLE);
				fixCan.setVisibility(View.INVISIBLE);
				fixMotorCon1.setVisibility(View.INVISIBLE);
				fixMotorCon2.setVisibility(View.INVISIBLE);
				fixAccelerator.setVisibility(View.INVISIBLE);
				fixAccelerator.setVisibility(View.INVISIBLE);
			}
		});

		TSRunCan.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				Dismiss.setVisibility(View.VISIBLE);
				gray.setVisibility(View.VISIBLE);
				fixCan.setVisibility(View.VISIBLE);
			}
		});

		TSmotorcon1.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				Dismiss.setVisibility(View.VISIBLE);
				gray.setVisibility(View.VISIBLE);
				fixMotorCon1.setVisibility(View.VISIBLE);
			}
		});

		TSmotorcon2.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				Dismiss.setVisibility(View.VISIBLE);
				gray.setVisibility(View.VISIBLE);
				fixMotorCon2.setVisibility(View.VISIBLE);
			}
		});

		TSPostRunAccelerator.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				Dismiss.setVisibility(View.VISIBLE);
				gray.setVisibility(View.VISIBLE);
				fixAccelerator.setVisibility(View.VISIBLE);
			}
		});

		return rootView;
	}

}