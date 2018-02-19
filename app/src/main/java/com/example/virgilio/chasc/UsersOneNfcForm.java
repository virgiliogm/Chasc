package com.example.virgilio.chasc;

import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
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
import android.widget.TextView;
import android.widget.Toast;

import java.math.BigInteger;

public class UsersOneNfcForm extends AppCompatActivity {
    private DBUtil db;
    private TextView tvUser, tvNfc;
    private User user;

    // list of NFC technologies detected:
    private final String[][] TECHLIST = new String[][] {
        new String[] {
            NfcA.class.getName(),
            NfcB.class.getName(),
            NfcF.class.getName(),
            NfcV.class.getName(),
            IsoDep.class.getName(),
            MifareClassic.class.getName(),
            MifareUltralight.class.getName(), Ndef.class.getName()
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_users_one_nfc_form);

        // Show back button on top bar
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        db = new DBUtil();

        tvUser = (TextView) findViewById(R.id.tvUser);
        tvNfc = (TextView) findViewById(R.id.tvNfc);

        //check if NFC is enabled
        NfcManager manager = (NfcManager) getSystemService(Context.NFC_SERVICE);
        NfcAdapter adapter = manager.getDefaultAdapter();
        if (adapter != null && adapter.isEnabled()) {
            Bundle b = getIntent().getExtras();
            if (b != null) loadUser(b.getInt("id"));
            else finish();
        } else {
            //If NFC is disabled, show alert
            new AlertDialog.Builder(this)
                .setTitle(getString(R.string.nfc_disabled))
                .setMessage(getString(R.string.enable_nfc_admin))
                .setPositiveButton(getString(R.string.accept),
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            finish();
                        }
                    })
                .show();
        }
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
            // On NFC readed: update User and set text to the corresponding EditText

            String nfcId = ByteArrayToHexString(intent.getByteArrayExtra(NfcAdapter.EXTRA_ID));
            user.setNfc(nfcId);
            if (db.userMod(user)) tvNfc.setText(nfcId);
            else Toast.makeText(this, getString(R.string.error_mod_user), Toast.LENGTH_LONG).show();
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

    private void loadUser(int uid) {
        user = db.userById(uid);

        tvUser.setText(user.toString());
        tvNfc.setText(user.getNfc());
    }

    public void cancel (View v){
        finish();
    }
}
