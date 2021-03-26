package sae.iit.saedashboard;


import androidx.core.util.Pair;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class MyPagerAdapter extends FragmentStatePagerAdapter {
    List<Pair<Fragment, String>> list = new ArrayList<>();

    public MyPagerAdapter(FragmentManager fm) {
        super(fm, FragmentStatePagerAdapter.BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);
        list.add(new Pair<>(new MainTab(), "Drive"));
        list.add(new Pair<>(new SecondaryTab(), "Secondary"));
        list.add(new Pair<>(new DataLogTab(), "Data Logs"));
        list.add(new Pair<>(new TroubleshootTab(), "Troubleshooting"));
    }

    @Override
    public @NotNull Fragment getItem(int position) {
        return list.get(position).first;
    }

    @Override
    public int getCount() {
        return list.size();
    }

    @Override
    public CharSequence getPageTitle(int position) {
        return list.get(position).second;
    }

}