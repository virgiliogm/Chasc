package com.example.virgilio.chasc;

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
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.TextView;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class PublicMain extends AppCompatActivity {
    private DBUtil db;
    private SearchView sv;
    private TextView tvNoUsers;
    private ListView list;
    private String searchString = "";
    List<User> usersList;
    List<Schedule> schedulesList;

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
        setContentView(R.layout.activity_public_main);

        // Show back button on top bar
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        db = new DBUtil();
        list = (ListView) findViewById(R.id.list);
        tvNoUsers = (TextView) findViewById(R.id.tvNoUsers);

        // Check if NFC is enabled
        NfcManager manager = (NfcManager) getSystemService(Context.NFC_SERVICE);
        NfcAdapter adapter = manager.getDefaultAdapter();
        if (adapter == null || !adapter.isEnabled()) {
            //If NFC is disabled, show alert
            new AlertDialog.Builder(this)
                .setTitle(getString(R.string.nfc_disabled))
                .setMessage(getString(R.string.enable_nfc_public))
                .setPositiveButton(getString(R.string.accept), null)
                .show();
        }

        sv = (SearchView) findViewById(R.id.sv);
        sv.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                searchString = newText;
                loadUsers();
                return false;
            }
        });

        loadUsers();
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Enable NFC reading
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);

        IntentFilter filterIntent = new IntentFilter();
        filterIntent.addAction(NfcAdapter.ACTION_TAG_DISCOVERED);
        filterIntent.addAction(NfcAdapter.ACTION_NDEF_DISCOVERED);
        filterIntent.addAction(NfcAdapter.ACTION_TECH_DISCOVERED);

        NfcAdapter nfcAdapter = NfcAdapter.getDefaultAdapter(this);
        nfcAdapter.enableForegroundDispatch(this, pendingIntent, new IntentFilter[]{filterIntent}, this.TECHLIST);
    }

    @Override
    protected void onPause() {
        super.onPause();

        // Disable NFC reading
        NfcAdapter nfcAdapter = NfcAdapter.getDefaultAdapter(this);
        nfcAdapter.disableForegroundDispatch(this);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        if (intent.getAction().equals(NfcAdapter.ACTION_TAG_DISCOVERED)) {
            // On NFC readed: get ID, check if belongs to an authorized user and show Dialog

            String nfcId = ByteArrayToHexString(intent.getByteArrayExtra(NfcAdapter.EXTRA_ID));

            boolean auth = false;
            User user = db.userByNfc(nfcId);

            // Check if exists a user linked to this NFC device
            if (user != null) {

                // Check if user is in today's schedules list
                int index = usersList.indexOf(user);
                if (index != -1) {

                    // Schedule index in schedulesListis is the same than User index in usersList
                    Schedule schedule = schedulesList.get(index);

                    // Mark it as attended in database and update List
                    if (db.scheduleTick(schedule, Schedule.Event.ATTENDED)) {
                        schedule.setEvent(Schedule.Event.ATTENDED);
                        schedulesList.set(index, schedule);
                        list.setAdapter(new PublicUsersListAdapter(usersList, schedulesList, this));
                        auth = true;
                    }
                }
            }

            // Show dialog
            showAuthDialog(user, auth);
        }
    }

    private String ByteArrayToHexString(byte[] byteArray) {
        //Transform byteArray to String
        return String.format("%0" + (byteArray.length * 2) + "X", new BigInteger(1, byteArray));
    }

    // Finish activity on top bar back button click
    public boolean onOptionsItemSelected(MenuItem item){
        finish();
        return true;
    }

    private void loadUsers() {
        // Load users with schedules programmed for today (at 00:00:00.000)
        Calendar today = Calendar.getInstance();
        today.set(Calendar.HOUR_OF_DAY, 0);
        today.set(Calendar.MINUTE, 0);
        today.set(Calendar.SECOND, 0);
        today.set(Calendar.MILLISECOND, 0);

        // Load users with a Schedule programmed for the current date and mathing the search String
        usersList = db.usersByScheduleDate(today.getTime(), searchString);

        if (usersList == null || usersList.isEmpty()) {
            list.setVisibility(View.INVISIBLE);
            tvNoUsers.setVisibility(View.VISIBLE);
            if (searchString.isEmpty()) tvNoUsers.setText(getString(R.string.no_users));
            else tvNoUsers.setText(getString(R.string.no_results));
        } else {
            // Initialize and fill the Schedules List
            schedulesList = new ArrayList<>();
            for (User user : usersList) {
                schedulesList.add(user.getSchedule(today.getTime()));
            }

            tvNoUsers.setVisibility(View.INVISIBLE);
            list.setVisibility(View.VISIBLE);
            list.setAdapter(new PublicUsersListAdapter(usersList, schedulesList, this));
        }
    }

    private void showAuthDialog(User user, boolean auth) {
        // Show Dialog

        String title = (auth)? getString(R.string.auth_user) : getString(R.string.noauth_user);
        String msg = (user == null)? getString(R.string.no_user_nfc) : user.toString();
        int bgcolor = (auth)? R.color.colorSuccess : R.color.colorDanger;

        AlertDialog.Builder dialog = new AlertDialog.Builder(this).setTitle(title).setMessage(msg);

        final AlertDialog alert = dialog.show();
        alert.getWindow().setBackgroundDrawableResource(bgcolor);

        // Hide dialog after 2 seconds
        final Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            public void run() {
                alert.dismiss();
                timer.cancel();
            }
        }, 2000);
    }
}
