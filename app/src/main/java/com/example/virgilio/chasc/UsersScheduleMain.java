package com.example.virgilio.chasc;

import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.SparseArray;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import com.github.sundeepk.compactcalendarview.CompactCalendarView;
import com.github.sundeepk.compactcalendarview.domain.Event;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class UsersScheduleMain extends AppCompatActivity {
    private DBUtil db;
    private TextView tvDate;
    private CompactCalendarView calendar;
    private Calendar calFrom, calTo;
    private SimpleDateFormat fulldateFormat = new SimpleDateFormat("dd/MM/yyyy");
    private SimpleDateFormat monthyearFormat = new SimpleDateFormat("MMMM yyyy");

    // List of the first day of each already loaded month
    private List<Date> loaded;

    // Map to link each schedule ID to its event
    private SparseArray<Event> scheduleEvent;

    // Map to link each userID-User
    private SparseArray<User> users;

    // Map to link each event to its user
    private Map<Event, User> eventUser;

    // Variable used to delete all user schedules with the same ref token
    private boolean delAll = false;

    String deviceLanguage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_users_schedule_main);

        // Show back button on top bar
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // Get device language
        deviceLanguage = Locale.getDefault().getLanguage();

        db = new DBUtil();

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
                setCalendarData(firstDayOfNewMonth);
            }
        });

        tvDate = (TextView) findViewById(R.id.tvDate);
        tvDate.setTypeface(null, Typeface.BOLD);

        calFrom = Calendar.getInstance();
        calTo = Calendar.getInstance();

        loaded = new ArrayList<>();
        scheduleEvent = new SparseArray<>();
        users = new SparseArray<>();
        eventUser = new HashMap<>();

        setCalendarData(new Date());
    }

    @Override
    protected void onResume() {
        super.onResume();

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

    // Finish activity on top bar back button click
    public boolean onOptionsItemSelected(MenuItem item){
        finish();
        return true;
    }

    public String getDateText(Date date){
        String dateString = monthyearFormat.format(date.getTime());

        // Parse Catalan month names represented as "de <month_name>" or "d'<month_name>" removing this initial text
        if (deviceLanguage.equals("ca")) dateString = dateString.replaceAll("^d[e]?\\W?", "");

        // Parse date string to force first letter to uppercase
        dateString = dateString.substring(0, 1).toUpperCase() + dateString.substring(1);

        return dateString;
    }

    private void setCalendarData(Date date) {
        tvDate.setText(getDateText(date));

        calFrom.setTime(date);
        calFrom.set(Calendar.DAY_OF_MONTH, calFrom.getActualMinimum(Calendar.DAY_OF_MONTH));
        calFrom.set(Calendar.HOUR_OF_DAY, 0);
        calFrom.set(Calendar.MINUTE, 0);
        calFrom.set(Calendar.SECOND, 0);
        calFrom.set(Calendar.MILLISECOND, 0);

        calTo = (Calendar) calFrom.clone();
        calTo.set(Calendar.DAY_OF_MONTH, calTo.getActualMaximum(Calendar.DAY_OF_MONTH));

        // Check if the data of the selected month had already been loaded
        if (!loaded.contains(calFrom.getTime())) {
            loaded.add(calFrom.getTime());

            // Get schedules programmed for the selected month
            List<Schedule> schedules = db.schedulesGet(calFrom.getTime(), calTo.getTime());

            // Get the corresponding users
            List<User> usersList = db.usersByScheduleDates(calFrom.getTime(), calTo.getTime());

            // Insert users into the ID-User Map
            for (User user : usersList) {
                users.put(user.getId(), user);
            }

            // Add events to calendar and fill the corresponding Maps
            for (Schedule sch : schedules) {
                User user = users.get(sch.getUserId());

                String data = user.toString();
                if (sch.getEvent() != null) {
                    String event = (sch.getEvent() == Schedule.Event.ATTENDED)? getString(R.string.attended) : getString(R.string.no_show);
                    data += " - " + event;
                }

                Event evt = new Event(Color.BLACK, sch.getDate().getTime(), data);

                calendar.addEvent(evt, false);
                scheduleEvent.put(sch.getId(), evt);
                eventUser.put(evt, user);
            }

            // Redraw calendar events
            calendar.invalidate();
        }
    }

    private void showEventsDialog(final List<Event> events) {
        // Sort Events list alphabetically
        Collections.sort(events, new Comparator<Event>(){
            @Override
            public int compare(final Event a, Event b) {
                return a.getData().toString().compareTo(b.getData().toString());
            }
        });
        final String[] evtStrings = new String[events.size()];
        for (int i=0; i<events.size(); i++){
            evtStrings[i] = "\uD83D\uDDD1  " + events.get(i).getData().toString();
        }
        new AlertDialog.Builder(this)
            .setTitle(fulldateFormat.format(events.get(0).getTimeInMillis()) + " - " + getString(R.string.total) + ": " + events.size())
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
        User user = eventUser.get(event);
        final Context c = this;
        final Schedule sch = user.getSchedule(new Date(event.getTimeInMillis()));
        // Show dialog depending on schedule type: range or single day
        if (sch.getRef() != null && !sch.getRef().isEmpty()) {
            // If the schedule belongs to a range, user can delete just this date or the full range
            String[] options = {
                getString(R.string.del_single_day),
                getString(R.string.del_all_dates_range) + ":\n" + sch.getRefText()
            };

            new AlertDialog.Builder(this)
                .setCancelable(true)
                .setTitle(fulldateFormat.format(event.getTimeInMillis()) + " - " + user.toString())
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
                                // Remove events from calendar
                                for (Schedule sch : refSchedules) {
                                    Event evt = scheduleEvent.get(sch.getId());
                                    if (evt != null) calendar.removeEvent(evt, false);
                                }

                                // Redraw calendar events
                                calendar.invalidate();
                            } else Toast.makeText(c, getString(R.string.error_del_schedules), Toast.LENGTH_LONG).show();

                            // Reset delAll variable to false
                            delAll = false;

                        } else {
                            // Delete the selected schedule
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
                .setTitle(fulldateFormat.format(event.getTimeInMillis()) + " - " + user.toString())
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

}
