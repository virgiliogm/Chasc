package tk.rktdev.chasc;

import android.app.Dialog;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.widget.Button;
import android.widget.EditText;

import java.util.Timer;
import java.util.TimerTask;

public class UserFormDialog extends Dialog {
    private TabsActivity activity;
    private EditText etName, etSurname, etNfc;
    private Button btnOK, btnKO;

    public UserFormDialog(TabsActivity a) {
        super(a);
        this.activity = a;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_user_form);

        etName = (EditText) findViewById(tk.rktdev.chasc.R.id.etName);
        etSurname = (EditText) findViewById(tk.rktdev.chasc.R.id.etSurname);
        etNfc = (EditText) findViewById(tk.rktdev.chasc.R.id.etNfc);
        btnOK = (Button) findViewById(tk.rktdev.chasc.R.id.btnOK);
        btnKO = (Button) findViewById(tk.rktdev.chasc.R.id.btnKO);
    }

    public void setData(User user) {
        etName.setText(user.getName());
        etSurname.setText(user.getSurname());
        etNfc.setText(user.getNfc());
    }

    public void setNfc(String nfcId) {
        etNfc.setText(nfcId);

        // Highlight NFC EditText
        int color = ContextCompat.getColor(activity, tk.rktdev.chasc.R.color.colorSuccess);
        etNfc.getBackground().setColorFilter(color, PorterDuff.Mode.SRC);

        // Remove highlight after 1 second
        final Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                // Need to use runOnUiThread() to modify a view from other thread than the original one
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        etNfc.getBackground().clearColorFilter();
                        timer.cancel();
                    }
                });
            }
        }, 1000);
    }

    public String getName() {
        return etName.getText().toString();
    }

    public String getSurname() {
        return etSurname.getText().toString();
    }

    public String getNfc() {
        return etNfc.getText().toString();
    }

    public void enableButtons() {
        btnOK.setEnabled(true);
        btnKO.setEnabled(true);
    }

    public void disableButtons() {
        btnOK.setEnabled(false);
        btnKO.setEnabled(false);
    }
}
