package com.example.virgilio.chasc;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UsersOneScheduleForm extends AppCompatActivity {
    private DBUtil db;
    private RadioButton rbRange, rbDay;
    private EditText etFrom, etTo;
    private TextView tvUser, tvFrom, tvTo;
    private LinearLayout dowButtons;
    private CheckBox cbAlt;

    private Map<String, Integer> activeDows;
    private String[] dows = {"Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"};
    private Calendar calFrom, calTo;
    private String format = "dd/MM/yyyy";
    private SimpleDateFormat dateFormat = new SimpleDateFormat(format);

    private User user;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_users_one_schedule_form);

        // Show back button on top bar
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        db = new DBUtil();

        tvUser = (TextView) findViewById(R.id.tvUser);

        Bundle b = getIntent().getExtras();
        if (b != null) loadUser(b.getInt("id"));
        else finish();

        // Init Calendar variables and set click listener and text to corresponding EditText
        calFrom = Calendar.getInstance();
        calFrom.set(Calendar.HOUR_OF_DAY, 0);
        calFrom.set(Calendar.MINUTE, 0);
        calFrom.set(Calendar.SECOND, 0);
        calFrom.set(Calendar.MILLISECOND, 0);
        tvFrom = (TextView) findViewById(R.id.tvFrom);
        etFrom = (EditText) findViewById(R.id.etFrom);
        etFrom.setText(dateFormat.format(calFrom.getTime()));
        etFrom.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new DatePickerDialog(UsersOneScheduleForm.this, new DatePickerDialog.OnDateSetListener() {
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
        tvTo = (TextView) findViewById(R.id.tvTo);
        etTo = (EditText) findViewById(R.id.etTo);
        etTo.setText(dateFormat.format(calTo.getTime()));
        etTo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new DatePickerDialog(UsersOneScheduleForm.this, new DatePickerDialog.OnDateSetListener() {
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

        // Manage which elements hide or show when Range or Day RadioButton is selected

        dowButtons = (LinearLayout) findViewById(R.id.dows);
        cbAlt = (CheckBox) findViewById(R.id.cbAlt);

        rbDay = (RadioButton) findViewById(R.id.rbDay);
        rbDay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                tvFrom.setText(getString(R.string.day));
                tvTo.setVisibility(View.INVISIBLE);
                etTo.setVisibility(View.INVISIBLE);
                dowButtons.setVisibility(View.INVISIBLE);
                cbAlt.setVisibility(View.INVISIBLE);
            }
        });

        rbRange = (RadioButton) findViewById(R.id.rbRange);
        rbRange.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                tvFrom.setText(getString(R.string.from_uppercase));
                tvTo.setVisibility(View.VISIBLE);
                etTo.setVisibility(View.VISIBLE);
                dowButtons.setVisibility(View.VISIBLE);
                cbAlt.setVisibility(View.VISIBLE);
                initDows();
            }
        });
        initDows();

        // Set click listener to each Days of the week CheckBox
        for (final String dow : dows) {
            final CheckBox cb = (CheckBox) findViewById(getResources().getIdentifier("com.example.virgilio.chasc:id/cb" + dow, null, null));
            cb.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    boolean checked = cb.isChecked();
                    setDow(dow, checked);
                }
            });
        }
    }

    // Finish activity on top bar back button click
    public boolean onOptionsItemSelected(MenuItem item){
        finish();
        return true;
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
        CheckBox cb = (CheckBox) findViewById(getResources().getIdentifier("com.example.virgilio.chasc:id/cb"+name, null, null));
        int n = (checked)? 1 : 0;
        int color = (checked)? ContextCompat.getColor(this, R.color.colorSuccess) : ContextCompat.getColor(this, R.color.colorDanger);

        cb.setChecked(checked);
        cb.setBackgroundColor(color);
        activeDows.put(name, n);
    }

    private void loadUser(int uid) {
        user = db.userById(uid);
        tvUser.setText(user.toString());
    }

    private void checkRange() {
        // Check if range dates ar valid: To has to be greater than From but no more than 365 days
        int dayFrom = calFrom.get(Calendar.DAY_OF_YEAR);
        int dayTo = calTo.get(Calendar.DAY_OF_YEAR);
        int yearsDiff = calTo.get(Calendar.YEAR) - calFrom.get(Calendar.YEAR);
        if (yearsDiff != 0) dayTo += 365 * yearsDiff;

        if (dayFrom > dayTo || dayTo - dayFrom > 365){
            calTo = (Calendar) calFrom.clone();
            etTo.setText(dateFormat.format(calTo.getTime()));
        }
    }

    private String getRefText() {
        String res = "";

        //Create a list with the active dows names iterating dows array to preserve the order
        List <String> validDows = new ArrayList<>();
        for (String dow : dows){
            String dowName = "";
            if (dow.equals("Mon")) dowName = getString(R.string.dow_monday);
            else if (dow.equals("Tue")) dowName = getString(R.string.dow_tuesday);
            else if (dow.equals("Wed")) dowName = getString(R.string.dow_wednesday);
            else if (dow.equals("Thu")) dowName = getString(R.string.dow_thursday);
            else if (dow.equals("Fri")) dowName = getString(R.string.dow_friday);
            else if (dow.equals("Sat")) dowName = getString(R.string.dow_saturday);
            else if (dow.equals("Sun")) dowName = getString(R.string.dow_sunday);
            if (activeDows.get(dow) == 1) validDows.add(dowName);
        }

        //Concatenate the active dows
        if (validDows.size() == 1) res += validDows.get(0);
        else for (int i=0; i<validDows.size(); i++) {
            res += validDows.get(i);
            if (i < validDows.size() - 2) res += ", ";
            else if (i == validDows.size() - 2) res += " " + getString(R.string.and) + " ";
        }

        //Concatenate the dates
        res += " " + getString(R.string.from_lowercase) + " " + dateFormat.format(calFrom.getTime()) + " " + getString(R.string.to_lowercase) + " " + dateFormat.format(calTo.getTime());

        //Concatenate if applies to alternate weeks
        if (cbAlt.isChecked()) res += " [" + getString(R.string.alternate_weeks) + "]";

        return res;
    }

    private boolean isBeforeOrEqual(Calendar cal1, Calendar cal2) {
        return (cal1.get(Calendar.YEAR) < cal2.get(Calendar.YEAR) ||
                (cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                 cal1.get(Calendar.DAY_OF_YEAR) <= cal2.get(Calendar.DAY_OF_YEAR)));
    }

    private boolean isValidWeek(Calendar cal1) {
        // Check if the Calendar object week is valid in the selected range (util when alternate-weeks option is selected)
        boolean res = false;
        if (!cbAlt.isChecked()) res = true;
        else {
            //If the alternate-weeks CheckBox is checked, check if both current and received Calendar weeks are even or odd
            int startWeek = calFrom.get(Calendar.WEEK_OF_YEAR);
            int currWeek = cal1.get(Calendar.WEEK_OF_YEAR);
            if ((startWeek % 2 == 0 && currWeek % 2 == 0) ||
                (startWeek % 2 != 0 && currWeek % 2 != 0)) res = true;
        }
        return res;
    }

    public void confirm (View v){
        if (rbRange.isChecked()) {
            //Range of dates
            List<Schedule> schedules = new ArrayList<>();
            String ref = new Token().toString();
            String refText = getRefText();

            Calendar cal = (Calendar) calFrom.clone();
            Calendar end = (Calendar) calTo.clone();

            for (Date date = cal.getTime(); isBeforeOrEqual(cal, end); cal.add(Calendar.DATE, 1), date = cal.getTime()) {
                // Loop from start to end of the range selecting each valid day of the week of each valid week
                if (isValidWeek(cal)) {
                    int dow = cal.get(Calendar.DAY_OF_WEEK) - 2; // it returns 2 for Monday & we want it to be 0 like the dows array index
                    if (dow == -1) dow = 6;  // Sunday was originally 1 & we want it to be 6
                    CheckBox cb = (CheckBox) findViewById(getResources().getIdentifier("com.example.virgilio.chasc:id/cb"+dows[dow], null, null));
                    if (cb.isChecked()) schedules.add(new Schedule(-1, user, date, ref, refText));
                }
            }

            if (schedules.isEmpty()) Toast.makeText(this, getString(R.string.dates_not_valid), Toast.LENGTH_LONG).show();
            else if (db.schedulesAdd(user, schedules)) finish();
            else Toast.makeText(this, getString(R.string.error_add_schedule), Toast.LENGTH_LONG).show();
        } else {
            //Single day
            if (db.scheduleAdd(user, new Schedule(-1, user, calFrom.getTime()))) finish();
            else Toast.makeText(this, getString(R.string.error_add_schedule), Toast.LENGTH_LONG).show();
        }
    }

    public void cancel (View v){
        finish();
    }
}
