package tk.rktdev.chasc;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

public class TabsManager extends FragmentPagerAdapter {
    private TabUsers tabUsers;
    private TabCalendar tabCalendar;
    private TabLunchTime tabLunchTime;
    private TabSettings tabSettings;

    public TabsManager(FragmentManager fm) {
        super(fm);
        tabUsers = new TabUsers();
        tabCalendar = new TabCalendar();
        tabLunchTime = new TabLunchTime();
        tabSettings = new TabSettings();
    }

    @Override
    public Fragment getItem(int position) {
        switch (position) {
            case 0:
                return tabUsers;
            case 1:
                return tabCalendar;
            case 2:
                return tabLunchTime;
            case 3:
                return tabSettings;
            default:
                return null;
        }
    }

    @Override
    public int getCount() {
        return 4;
    }

    public int getPosition(String tab) {
        switch (tab) {
            case "users":
                return 0;
            case "calendar":
                return 1;
            case "lunchtime":
                return 2;
            case "settings":
                return 3;
            default:
                return 0;
        }
    }
}
