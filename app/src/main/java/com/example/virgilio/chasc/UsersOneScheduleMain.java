package com.example.virgilio.chasc;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.SparseArray;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.github.sundeepk.compactcalendarview.CompactCalendarView;
import com.github.sundeepk.compactcalendarview.domain.Event;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class UsersOneScheduleMain extends AppCompatActivity {
    private DBUtil db;
    private User user;
    private int uid;
    private TextView tvUser, tvDate;
    private CompactCalendarView calendar;
    private SimpleDateFormat fulldateFormat = new SimpleDateFormat("dd/MM/yyyy");
    private SimpleDateFormat monthyearFormat = new SimpleDateFormat("MMMM yyyy");

    //Map to link each schedule ID to its calendar event
    private SparseArray<Event> scheduleEvent;

    //Variable used to delete all user schedules with the same ref token
    private boolean delAll = false;

    String deviceLanguage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_users_one_schedule_main);

        // Show back button on top bar
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // Get device language
        deviceLanguage = Locale.getDefault().getLanguage();

        tvUser = (TextView) findViewById(R.id.tvUser);

        tvDate = (TextView) findViewById(R.id.tvDate);
        tvDate.setTypeface(null, Typeface.BOLD);
        tvDate.setText(getDateText(new Date()));

        calendar = (CompactCalendarView) findViewById(R.id.cal);
        calendar.setCurrentDayBackgroundColor(ContextCompat.getColor(this, R.color.colorCurrDay));
        calendar.setCurrentSelectedDayBackgroundColor(ContextCompat.getColor(this, R.color.colorSelDay));
        calendar.shouldDrawIndicatorsBelowSelectedDays(true);

        calendar.setListener(new CompactCalendarView.CompactCalendarViewListener() {
            @Override
            public void onDayClick(Date dateClicked) {
                List<Event> events = calendar.getEvents(dateClicked);
                if (!events.isEmpty()) showEventsDialog(events);
            }

            @Override
            public void onMonthScroll(Date firstDayOfNewMonth) {
                tvDate.setText(getDateText(firstDayOfNewMonth));
            }
        });

        scheduleEvent = new SparseArray<>();

        db = new DBUtil();

        Bundle b = getIntent().getExtras();
        if (b != null){
            uid = b.getInt("id");
            loadUser();
        }
        else finish();
    }

    @Override
    protected void onResume() {
        super.onResume();
        calendar.removeAllEvents();
        loadUser();

        // Get device language
        deviceLanguage = Locale.getDefault().getLanguage();

        // Customize Spanish and Catalan days-of-the-week names
        if (deviceLanguage.equals("ca")) {
            calendar.setUseThreeLetterAbbreviation(true);
            String[] dowNames = {"Dl", "Dm", "Dc", "Dj", "Dv", "Ds", "Dg"};
            calendar.setDayColumnNames(dowNames);
        } else if (deviceLanguage.equals("es")) {
            String[] dowNames = {"L", "M", "X", "J", "V", "S", "D"};
            calendar.setDayColumnNames(dowNames);
        }
    }

    public String getDateText(Date date){
        String dateString = monthyearFormat.format(date.getTime());

        // Parse Catalan month names represented as "de <month_name>" or "d'<month_name>" removing this initial text
        if (deviceLanguage.equals("ca")) dateString = dateString.replaceAll("^d[e]?\\W?", "");

        // Parse date string to force first letter to uppercase
        dateString = dateString.substring(0, 1).toUpperCase() + dateString.substring(1);

        return dateString;
    }

    // Finish activity on top bar back button click
    public boolean onOptionsItemSelected(MenuItem item){
        finish();
        return true;
    }

    private void loadUser() {
        user = db.userById(uid);
        tvUser.setText(user.toString());

        // Add events to calendar and fill the corresponding Map
        for (Schedule sch : user.getSchedules()) {
            String data = user.toString();
            if (sch.getEvent() != null) {
                String event = (sch.getEvent() == Schedule.Event.ATTENDED)? getString(R.string.attended) : getString(R.string.no_show);
                data += " - " + event;
            }
            if (sch.getRefText() != null && !sch.getRefText().isEmpty()) data += " (" + sch.getRefText() + ")";

            Event evt = new Event(Color.BLACK, sch.getDate().getTime(), data);

            calendar.addEvent(evt, false);
            scheduleEvent.put(sch.getId(), evt);
        }

        // Redraw calendar events
        calendar.invalidate();
    }

    private void showEventsDialog(final List<Event> events) {
        // Prepend unicode wastebasket icon to event data
        final String[] evtStrings = {"\uD83D\uDDD1  " + events.get(0).getData().toString()};

        new AlertDialog.Builder(this)
            .setTitle(fulldateFormat.format(events.get(0).getTimeInMillis()))
            .setItems(evtStrings, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    showDelDialog(events.get(which));
                }
            })
            .setNegativeButton(getString(R.string.close), null)
            .show();
    }

    private void showDelDialog(final Event event) {
        final Context c = this;
        final Schedule sch = user.getSchedule(new Date(event.getTimeInMillis()));
        if (sch.getRef() != null && !sch.getRef().isEmpty()) {
            // If the schedule belongs to a range, user can delete just this date or the full range
            String[] options = {
                getString(R.string.del_single_day),
                getString(R.string.del_all_dates_range) + ":\n" + sch.getRefText()
            };

            new AlertDialog.Builder(this)
                .setCancelable(true)
                .setTitle(fulldateFormat.format(event.getTimeInMillis()))
                .setSingleChoiceItems(options, 0, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        delAll = (which == 1);
                    }
                })
                .setPositiveButton(getString(R.string.confirm), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (delAll) {
                            //Delete all schedules with the same ref token
                            List<Schedule> refSchedules = db.schedulesGet(sch.getRef());
                            if (db.schedulesDel(sch.getRef())) {
                                for (Schedule sch : refSchedules){
                                    // For each one, remove the event from the calendar, without redrawing it
                                    calendar.removeEvent(scheduleEvent.get(sch.getId()), false);
                                }

                                //Redraw all calendar events
                                calendar.invalidate();
                            } else Toast.makeText(c, getString(R.string.error_del_schedules), Toast.LENGTH_LONG).show();

                            //Reset delAll variable to false
                            delAll = false;

                        } else {
                            //Delete the selected schedule
                            if (db.scheduleDel(sch)) calendar.removeEvent(event, true);
                            else Toast.makeText(c, getString(R.string.error_del_schedule), Toast.LENGTH_LONG).show();
                        }
                    }
                })
                .setNegativeButton(getString(R.string.cancel), null)
                .show();
        } else {
            new AlertDialog.Builder(this)
                .setCancelable(true)
                .setTitle(fulldateFormat.format(event.getTimeInMillis()))
                .setMessage(getString(R.string.del_schedule) + ". " + getString(R.string.sure))
                .setPositiveButton(getString(R.string.confirm), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (db.scheduleDel(sch)) calendar.removeEvent(event, true);
                        else Toast.makeText(c, getString(R.string.error_del_schedule), Toast.LENGTH_LONG).show();
                    }
                })
                .setNegativeButton(getString(R.string.cancel), null)
                .show();
        }
    }

    public void add(View view) {
        Intent intent = new Intent(this, UsersOneScheduleForm.class);
        Bundle b = new Bundle();
        b.putInt("id", user.getId());
        intent.putExtras(b);
        startActivity(intent);
    }
}
