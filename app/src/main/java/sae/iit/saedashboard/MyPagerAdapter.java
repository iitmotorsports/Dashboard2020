package sae.iit.saedashboard;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;

import org.jetbrains.annotations.NotNull;

public class MyPagerAdapter extends FragmentStatePagerAdapter {

    MainTab p0;
    SecondaryTab p1;
    TroubleshootTab p2;

    public MyPagerAdapter(FragmentManager fm) {
        //Creates Fragment Adapter
        super(fm, FragmentStatePagerAdapter.BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);
        p0 = new MainTab();
        p1 = new SecondaryTab();
        p2 = new TroubleshootTab();
    }

    @Override
    public @NotNull Fragment getItem(int position) {
        //Generates the new tab
        switch (position) {
            case 0:
                return p0;
            case 1:
                return p1;
            case 2:
                return p2;

        }
        return p0;
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