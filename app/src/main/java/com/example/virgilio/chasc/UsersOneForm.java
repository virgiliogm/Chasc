package com.example.virgilio.chasc;

import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.NfcAdapter;
import android.nfc.tech.IsoDep;
import android.nfc.tech.MifareClassic;
import android.nfc.tech.MifareUltralight;
import android.nfc.tech.Ndef;
import android.nfc.tech.NfcA;
import android.nfc.tech.NfcB;
import android.nfc.tech.NfcF;
import android.nfc.tech.NfcV;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import java.math.BigInteger;

public class UsersOneForm extends AppCompatActivity {
    private DBUtil db;
    private EditText etName, etSurname, etNfc;
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
        setContentView(R.layout.activity_users_one_form);

        // Show back button on top bar
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        db = new DBUtil();

        etName = (EditText) findViewById(R.id.etName);
        etSurname = (EditText) findViewById(R.id.etSurname);
        etNfc = (EditText) findViewById(R.id.etNfc);

        Bundle b = getIntent().getExtras();
        if (b != null) loadUser(b.getInt("id"));
        else user = new User();
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
            // On NFC readed: set text to corresponding EditText

            String nfcId = ByteArrayToHexString(intent.getByteArrayExtra(NfcAdapter.EXTRA_ID));
            etNfc.setText(nfcId);
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

        etName.setText(user.getName());
        etSurname.setText(user.getSurname());
        etNfc.setText(user.getNfc());
    }

    public void saveUser (View v){
        String name = etName.getText().toString();
        String surname = etSurname.getText().toString();
        String nfc = etNfc.getText().toString();

        if (name.isEmpty()) {
            Toast.makeText(this, getString(R.string.insert_name), Toast.LENGTH_LONG).show();
        } else if (surname.isEmpty()) {
            Toast.makeText(this, getString(R.string.insert_surname), Toast.LENGTH_LONG).show();
        } else {
            user.setName(name);
            user.setSurname(surname);
            user.setNfc(nfc);

            if (user.getId() == -1) {
                if (db.userAdd(user)) finish();
                else Toast.makeText(this, getString(R.string.error_add_user), Toast.LENGTH_LONG).show();
            } else {
                if (db.userMod(user)) finish();
                else Toast.makeText(this, getString(R.string.error_mod_user), Toast.LENGTH_LONG).show();
            }
        }
    }
}
