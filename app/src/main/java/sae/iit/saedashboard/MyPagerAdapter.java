package sae.iit.saedashboard;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;

public class MyPagerAdapter extends FragmentStatePagerAdapter {

	public MyPagerAdapter(FragmentManager fm) {
		//Creates Fragment Adapter
		super(fm);
	}

	@Override
	public Fragment getItem(int position) {
		//Generates the new tab
		switch (position) {
			case 0:
				return new MainTab();
			case 1:
				return new SecondaryTab();
			case 2:
				return new TroubleshootTab();

		}
		return null;
	}

	@Override
	public int getCount() {
		return 3;
	}

	@Override
	public CharSequence getPageTitle(int position) {
		switch (position) {
			case 0:
				return "Main";
			case 1:
				return "DataLogging";
			case 2:
				return "Troubleshooting";
			default:
				return null;
		}
	}

}