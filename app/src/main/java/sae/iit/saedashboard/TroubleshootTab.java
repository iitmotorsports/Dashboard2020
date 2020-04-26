package sae.iit.saedashboard;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.PopupWindow;
import android.view.ViewGroup.LayoutParams;
import android.widget.TextView;
import androidx.fragment.app.Fragment;
import static android.content.Context.LAYOUT_INFLATER_SERVICE;

/*Designer: Deep Trapasia
SAE Electrical Team Member(Software Team) 2019-2020
 */
public class TroubleshootTab extends Fragment implements View.OnClickListener {

	public Button TSPostRunHardwareGate;
	public Button TSPostRunCurrentSensor;
	public Button TSPostRunAccelerator;
	public Button TSPostRunTemperature;
	public Button TSPostRunVoltageSensor;
	public Button TSPostRunPreCharge;
	public Button TSPostRunEEPROM;
	public Button TSPostRunBrake;
	public Button TSRunCan;
	public Button TSRunDisCharge;
	public Button TSRunBrake;
	public ViewGroup rootView;
	public TextView theScon;
	@Override
	public View onCreateView(final LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		theScon = (getActivity().findViewById(R.id.sconsole));

		return inflater.inflate(R.layout.troubleshoot_tab,container,false);
	}


	public void Faults(Button fault, Boolean check) {

		if(check == true) {
			fault.setBackgroundColor(0xA4FFEB3B);//change to yellow if fault
		} else {
			fault.setBackgroundColor(0xAE00FF03);//change to green if fault
		}
	}



//=================================================================================================
//Ignore this method....
	@Override
	public void onClick(View view) {
		LayoutInflater layoutInflater
				= (LayoutInflater) getContext()
				.getSystemService(LAYOUT_INFLATER_SERVICE);
		View popupView = layoutInflater.inflate(R.layout.popup_window, null);
		final PopupWindow popupWindow = new PopupWindow(
				popupView,
				LayoutParams.WRAP_CONTENT,
				LayoutParams.WRAP_CONTENT);

		Button btnDismiss =  popupView.findViewById(R.id.dismiss);
		btnDismiss.setOnClickListener(new Button.OnClickListener() {

			@Override
			public void onClick(View v) {
				popupWindow.dismiss();
			}
		});

	}
//================================================================================================


}