package tk.rktdev.chasc;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class LunchdateFormDialog extends Dialog {
    public TabsActivity activity;

    private RelativeLayout mainLayout;
    private RadioButton rbRange, rbDay;
    private EditText etFrom, etTo;
    private TextView tvUser, tvFrom;
    private LinearLayout dowButtons, rowTo, bottomButtons;
    private CheckBox cbAlt;
    private ViewGroup.LayoutParams rowToLayoutParams, dowButtonsLayoutParams, cbAltLayoutParams;
    private Button btnOK, btnKO;

    private Map<String, Integer> activeDows;
    private String[] dows = {"Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"};
    private Calendar calFrom, calTo;
    private String format = "dd/MM/yyyy";
    private SimpleDateFormat dateFormat = new SimpleDateFormat(format);

    public LunchdateFormDialog(TabsActivity a) {
        super(a);
        this.activity = a;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_lunchdate_form);

        mainLayout = (RelativeLayout) findViewById(R.id.lunchdateLayout);
        tvUser = (TextView) findViewById(R.id.tvUser);
        tvFrom = (TextView) findViewById(R.id.tvFrom);
        etFrom = (EditText) findViewById(R.id.etFrom);
        rowTo = (LinearLayout) findViewById(R.id.rowTo);
        etTo = (EditText) findViewById(R.id.etTo);
        dowButtons = (LinearLayout) findViewById(R.id.dows);
        cbAlt = (CheckBox) findViewById(R.id.cbAlt);
        rbDay = (RadioButton) findViewById(R.id.rbDay);
        rbRange = (RadioButton) findViewById(R.id.rbRange);
        bottomButtons = (LinearLayout) findViewById(R.id.bottomButtons);
        btnOK = (Button) findViewById(tk.rktdev.chasc.R.id.btnOK);
        btnKO = (Button) findViewById(tk.rktdev.chasc.R.id.btnKO);

        rowToLayoutParams = rowTo.getLayoutParams();
        dowButtonsLayoutParams = dowButtons.getLayoutParams();
        cbAltLayoutParams = cbAlt.getLayoutParams();

        // Init Calendar objects and set click listener and text to the corresponding EditText
        calFrom = Calendar.getInstance();
        calFrom.set(Calendar.HOUR_OF_DAY, 0);
        calFrom.set(Calendar.MINUTE, 0);
        calFrom.set(Calendar.SECOND, 0);
        calFrom.set(Calendar.MILLISECOND, 0);
        etFrom.setText(dateFormat.format(calFrom.getTime()));
        etFrom.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new DatePickerDialog(activity, new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                        calFrom.set(Calendar.YEAR, year);
                        calFrom.set(Calendar.MONTH, monthOfYear);
                        calFrom.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                        checkRange();
                        etFrom.setText(dateFormat.format(calFrom.getTime()));
                    }
                }, calFrom.get(Calendar.YEAR), calFrom.get(Calendar.MONTH), calFrom.get(Calendar.DAY_OF_MONTH)).show();
            }
        });
        calTo = (Calendar) calFrom.clone();
        etTo.setText(dateFormat.format(calTo.getTime()));
        etTo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new DatePickerDialog(activity, new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                        calTo.set(Calendar.YEAR, year);
                        calTo.set(Calendar.MONTH, monthOfYear);
                        calTo.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                        checkRange();
                        etTo.setText(dateFormat.format(calTo.getTime()));
                    }
                }, calTo.get(Calendar.YEAR), calTo.get(Calendar.MONTH), calTo.get(Calendar.DAY_OF_MONTH)).show();
            }
        });

        // Set click listener to RadioButtons
        rbDay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                changeType(v, false);
            }
        });
        rbRange.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                changeType(v, true);
            }
        });

        mainLayout.post(new Runnable() {
            @Override
            public void run() {
                // Set click listener to each Days of the week CheckBox & set height equal to width
                for (final String dow : dows) {
                    final CheckBox cb = (CheckBox) findViewById(activity.getResources().getIdentifier("cb" + dow, "id", activity.getPackageName()));
                    cb.setHeight(cb.getWidth());
                    cb.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            boolean checked = cb.isChecked();
                            setDow(dow, checked);
                        }
                    });
                }
            }
        });

        initDows();
    }

    private void changeType(View v, boolean range) {
        // Manage which elements hide or show when Range or Day RadioButton is selected
        ViewGroup.LayoutParams mainParams = mainLayout.getLayoutParams();

        RelativeLayout.LayoutParams bottomButtonsLayoutParams = new RelativeLayout.LayoutParams(bottomButtons.getLayoutParams());
        bottomButtonsLayoutParams.topMargin = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 20, activity.getResources().getDisplayMetrics());

        if (range) {
            tvFrom.setText(activity.getString(R.string.from_uppercase));
            ((ViewManager) v.getParent().getParent()).addView(rowTo, rowToLayoutParams);
            ((ViewManager) v.getParent().getParent()).addView(dowButtons, dowButtonsLayoutParams);
            ((ViewManager) v.getParent().getParent()).addView(cbAlt, cbAltLayoutParams);

            mainParams.height = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 440, activity.getResources().getDisplayMetrics());
            mainLayout.setLayoutParams(mainParams);

            bottomButtonsLayoutParams.addRule(RelativeLayout.BELOW, R.id.cbAlt);
            bottomButtons.setLayoutParams(bottomButtonsLayoutParams);

            initDows();
        } else {
            tvFrom.setText(activity.getString(R.string.day));
            ((ViewManager) v.getParent().getParent()).removeView(rowTo);
            ((ViewManager) v.getParent().getParent()).removeView(dowButtons);
            ((ViewManager) v.getParent().getParent()).removeView(cbAlt);

            mainParams.height = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 272, activity.getResources().getDisplayMetrics());
            mainLayout.setLayoutParams(mainParams);

            bottomButtonsLayoutParams.addRule(RelativeLayout.BELOW, R.id.rowFrom);
            bottomButtons.setLayoutParams(bottomButtonsLayoutParams);
        }
    }

    private void initDows() {
        activeDows = new HashMap<>();

        // Set true from Monday to Friday by default
        String[] defaultDows = {"Mon", "Tue", "Wed", "Thu", "Fri"};
        for (String dow : dows) {
            boolean check = (Arrays.asList(defaultDows).indexOf(dow) > -1);
            setDow(dow, check);
        }
    }

    private void setDow(String name, boolean checked) {
        // Get CheckBox element by dow name to dynamically change its attributes
        CheckBox cb = (CheckBox) findViewById(activity.getResources().getIdentifier("cb"+name, "id", activity.getPackageName()));
        int n = (checked)? 1 : 0;
        Drawable background = (checked)? ContextCompat.getDrawable(activity, R.drawable.oval_shape_success) : ContextCompat.getDrawable(activity, R.drawable.oval_shape_danger);

        cb.setChecked(checked);
        cb.setBackground(background);
        activeDows.put(name, n);
    }

    private void checkRange() {
        // Check if range dates ar valid: "To" has to be greater than "From" but no more than 365 days
        int dayFrom = calFrom.get(Calendar.DAY_OF_YEAR);
        int dayTo = calTo.get(Calendar.DAY_OF_YEAR);
        int yearsDiff = calTo.get(Calendar.YEAR) - calFrom.get(Calendar.YEAR);
        if (yearsDiff != 0) dayTo += 365 * yearsDiff;

        if (dayFrom > dayTo || dayTo - dayFrom > 365) {
            calTo = (Calendar) calFrom.clone();
            if (dayFrom > dayTo) {
                // If From is greater than To, set To equal to From
                Toast.makeText(activity, activity.getString(R.string.error_lunchdate_from_gt_to), Toast.LENGTH_SHORT).show();
            } else {
                // If range is more than 365 days long, set To to 1 year greater minus 1 day
                calTo.add(Calendar.YEAR, 1);
                calTo.add(Calendar.DATE, -1);
                Toast.makeText(activity, activity.getString(R.string.error_lunchdate_range_size), Toast.LENGTH_SHORT).show();
            }
            etTo.setText(dateFormat.format(calTo.getTime()));
        }
    }

    private void setDate(Date date, Calendar cal, EditText et) {
        cal.setTime(date);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        et.setText(dateFormat.format(cal.getTime()));
    }

    public void setData(User user, Date date) {
        tvUser.setText(user.toString());
        setDate(date, calFrom, etFrom);
        setDate(date, calTo, etTo);
    }

    public Boolean isRange() {
        return rbRange.isChecked();
    }

    public Calendar getFrom() {
        return calFrom;
    }

    public Calendar getTo() {
        return calTo;
    }

    public Map<String, Integer> getActiveDows() {
        return activeDows;
    }

    public Boolean isAlternateWeeks() {
        return cbAlt.isChecked();
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
