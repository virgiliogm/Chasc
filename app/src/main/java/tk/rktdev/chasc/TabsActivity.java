package tk.rktdev.chasc;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.NfcAdapter;
import android.nfc.NfcManager;
import android.nfc.tech.IsoDep;
import android.nfc.tech.MifareClassic;
import android.nfc.tech.MifareUltralight;
import android.nfc.tech.Ndef;
import android.nfc.tech.NfcA;
import android.nfc.tech.NfcB;
import android.nfc.tech.NfcF;
import android.nfc.tech.NfcV;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

public class TabsActivity extends AppCompatActivity {
    private TabsActivity self;

    private FirebaseUser account;
    private FirebaseFirestore db;

    private TabsManager tabsManager;
    private ViewPager viewPager;

    private TabUsers tabUsers;
    private TabCalendar tabCalendar;
    private TabLunchTime tabLunchTime;
    private TabSettings tabSettings;

    private NfcAdapter nfcAdapter;

    public List<User> allUsers;

    // List of NFC technologies detected:
    private final String[][] TECHLIST = new String[][] {
        new String[] {
            NfcA.class.getName(),
            NfcB.class.getName(),
            NfcF.class.getName(),
            NfcV.class.getName(),
            IsoDep.class.getName(),
            MifareClassic.class.getName(),
            MifareUltralight.class.getName(),
            Ndef.class.getName()
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tabs);

        self = this;

        account = FirebaseAuth.getInstance().getCurrentUser();
        if (account == null) finish();
        db = FirebaseFirestore.getInstance();

        // Create the adapter that will return a fragment for each of the sections of the activity.
        tabsManager = new TabsManager(getSupportFragmentManager());

        tabUsers = (TabUsers) tabsManager.getItem(0);
        tabCalendar = (TabCalendar) tabsManager.getItem(1);
        tabLunchTime = (TabLunchTime) tabsManager.getItem(2);
        tabSettings = (TabSettings) tabsManager.getItem(3);

        // Set up the ViewPager with the sections adapter.
        viewPager = (ViewPager) findViewById(R.id.container);
        viewPager.setAdapter(tabsManager);

        // Force to load all tabs at once & prevent them to be destroyed
        // Needed because we want to load just once the Users tab users list
        // & after that set Calendar tab users list & load Lunchtime tab lunchdates
        viewPager.setOffscreenPageLimit(3);

        TabLayout tabLayout = (TabLayout) findViewById(R.id.tabs);
        tabLayout.setupWithViewPager(viewPager);

        tabLayout.getTabAt(0).setIcon(R.drawable.ic_users);
        tabLayout.getTabAt(1).setIcon(R.drawable.ic_calendar);
        tabLayout.getTabAt(2).setIcon(R.drawable.ic_forkknife);
        tabLayout.getTabAt(3).setIcon(R.drawable.ic_settings);
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                switch (tab.getPosition()) {
                    case 0:
                        tabUsers.onEnter();
                        break;
                    case 1:
                        tabCalendar.onEnter();
                        break;
                    case 2:
                        tabLunchTime.onEnter();
                        break;
                }

                // Force keyboard to hide
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                View view = getCurrentFocus();
                if (view == null) {
                    view = new View(self);
                }
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
            }
        });

        NfcManager manager = (NfcManager) getSystemService(Context.NFC_SERVICE);
        nfcAdapter = manager.getDefaultAdapter();

        // Alert if NFC is disabled
        if (nfcAdapter == null || !nfcAdapter.isEnabled()) {
            new AlertDialog.Builder(this)
                .setTitle(getString(R.string.nfc_disabled))
                .setMessage(getString(R.string.enable_nfc))
                .setPositiveButton(getString(R.string.accept), null)
                .show();
        }

        allUsers = new ArrayList<>();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (nfcAdapter != null) {
            // Enable NFC reading
            PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);

            IntentFilter filterIntent = new IntentFilter();
            filterIntent.addAction(NfcAdapter.ACTION_TAG_DISCOVERED);
            filterIntent.addAction(NfcAdapter.ACTION_NDEF_DISCOVERED);
            filterIntent.addAction(NfcAdapter.ACTION_TECH_DISCOVERED);

            nfcAdapter.enableForegroundDispatch(this, pendingIntent, new IntentFilter[]{filterIntent}, this.TECHLIST);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (nfcAdapter != null) {
            // Disable NFC reading
            nfcAdapter.disableForegroundDispatch(this);
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        // Nfc read
        if (intent.getAction().equals(NfcAdapter.ACTION_TAG_DISCOVERED)) {
            String nfcId = byteArrayToHexString(intent.getByteArrayExtra(NfcAdapter.EXTRA_ID));

            switch (viewPager.getCurrentItem()) {
                case 0:
                    tabUsers.nfcRead(nfcId);
                    break;
                case 2:
                    tabLunchTime.nfcRead(nfcId);
                    break;
            }
        }
    }

    public String byteArrayToHexString(byte[] byteArray) {
        //Transform byteArray to String
        return String.format("%0" + (byteArray.length * 2) + "X", new BigInteger(1, byteArray));
    }

    public FirebaseFirestore getDb() {
        return db;
    }

    public FirebaseUser getAccount() {
        return account;
    }

    public void switchToTab(String tab) {
        viewPager.setCurrentItem(tabsManager.getPosition(tab));
    }


    ///////////
    // Users //
    ///////////

    public void showUserForm(View v) {
        tabUsers.showUserForm();
    }

    public void closeUserForm(View v) {
        tabUsers.closeUserForm();
    }

    public void saveUser(View v) {
        tabUsers.saveUser();
    }

    public void sortUsersList(List<User> list) {
        tabUsers.sortUsersList(list);
    }

    public User getUserById(String uid) {
        return tabUsers.getUserById(uid);
    }

    public void editUser(String uid) {
        tabUsers.editUser(uid);
    }

    public void delUser(String uid) {
        tabUsers.delUser(uid);
    }


    //////////////
    // Calendar //
    //////////////

    public void initCalendar() {
        tabCalendar.init();
    }

    public void showUsersListDialog(View v) {
        tabCalendar.showUsersListDialog();
    }

    public void closeUsersListDialog(View v) {
        tabCalendar.closeUsersListDialog();
    }

    public void showLunchdateForm(View v) {
        tabCalendar.showLunchdateForm();
    }

    public void closeLunchdateForm(View v) {
        tabCalendar.closeLunchdateForm();
    }

    public void saveLunchdate(View v) {
        tabCalendar.saveLunchdate();
    }

    public void setCalendarFilter(String uid) {
        tabCalendar.setFilter(getUserById(uid));
    }

    public void delCalendarFilter(View v) {
        tabCalendar.delFilter();
    }

    public void delLunchdateByUser(String uid) {
        tabCalendar.delLunchdateByUser(uid);
    }

    public void updateLunchdateStatus(Lunchdate lunchdate) {
        tabCalendar.updateLunchdateStatus(lunchdate);
    }


    ///////////////
    // LunchTime //
    ///////////////

    public void initLunchTime() {
        tabLunchTime.init();
    }

    public void addLunchdateToLunchTime(Lunchdate lunchdate) {
        tabLunchTime.addLunchdate(lunchdate);
    }

    public void addLunchdatesToLunchTime(List<Lunchdate> lunchdates) {
        tabLunchTime.addLunchdates(lunchdates);
    }

    public void delLunchdateFromLunchTime(Lunchdate lunchdate) {
        tabLunchTime.delLunchdate(lunchdate);
    }

    public void delLunchdatesFromLunchTime(List<Lunchdate> lunchdates) {
        tabLunchTime.delLunchdates(lunchdates);
    }

    public void showLunchTimeLoading() {
        tabLunchTime.showLoading();
    }

    public void hideLunchTimeLoading() {
        tabLunchTime.hideLoading();
    }


    //////////////
    // Settings //
    //////////////

    public void logout(View view) {
        tabSettings.logout();
    }

}
