package sae.iit.saedashboard;


import androidx.annotation.NonNull;
import androidx.core.util.Pair;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import java.util.ArrayList;
import java.util.List;

public class MyPagerAdapter extends FragmentStateAdapter {
    List<Pair<Fragment, String>> list = new ArrayList<>();

    public MyPagerAdapter(FragmentManager fm) {
        super(fm, new Lifecycle() {
            @Override
            public void addObserver(@NonNull LifecycleObserver observer) {

            }

            @Override
            public void removeObserver(@NonNull LifecycleObserver observer) {

            }

            @NonNull
            @Override
            public State getCurrentState() {
                return State.STARTED;
            }
        });
        list.add(new Pair<>(new MainTab(), "Drive"));
        list.add(new Pair<>(new SecondaryTab(), "Secondary"));
        list.add(new Pair<>(new DataLogTab(), "Data Logs"));
        list.add(new Pair<>(new PinoutTab(), "Pinout Tab"));
    }

//    @Override
//    public CharSequence getPageTitle(int position) {
//        return list.get(position).second;
//    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        Fragment get = list.get(position).first;
        if (get == null)
            return new MainTab();
        return get;
    }

    @Override
    public int getItemCount() {
        return list.size();
    }
}