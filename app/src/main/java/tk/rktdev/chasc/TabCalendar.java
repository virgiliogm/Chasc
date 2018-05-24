package tk.rktdev.chasc;

import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.github.sundeepk.compactcalendarview.CompactCalendarView;
import com.github.sundeepk.compactcalendarview.domain.Event;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.WriteBatch;

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

public class TabCalendar extends Fragment {
    private FirebaseFirestore db;
    private FirebaseUser account;

    private TabsActivity activity;
    private UsersListDialog usersListDialog;
    private LunchdateFormDialog lunchdateFormDialog;

    private String[] dows = {"Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"};
    private SimpleDateFormat fulldateFormat = new SimpleDateFormat("dd/MM/yyyy");
    private SimpleDateFormat monthyearFormat = new SimpleDateFormat("MMMM yyyy");

    private RelativeLayout loading;
    private TextView userFilter, tvDate;
    private Button btnAll, btnAdd;
    private CompactCalendarView calendar;
    private Calendar selectedDay;

    // List of the first day of each already loaded month
    private List<Date> loaded;

    // List of all the calendar events
    private List<Event> allEvents;

    // Map to link each lunchdate ID to its event
    private Map<String, Event> lunchdateEvent;

    // Map to link each event to its lunchdate
    private Map<Event, Lunchdate> eventLunchdate;

    // Map to link each event to its user
    private Map<Event, User> eventUser;

    // Variable used to delete all user lunchdates with the same ref token
    private boolean delAll = false;

    private String deviceLanguage;

    private User user;


    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.tab_calendar, container, false);
        activity = (TabsActivity) getActivity();

        db = activity.getDb();
        account = activity.getAccount();

        loading = (RelativeLayout) rootView.findViewById(R.id.loading);

        // Get device language
        deviceLanguage = Locale.getDefault().getLanguage();

        selectedDay = Calendar.getInstance();

        calendar = (CompactCalendarView) rootView.findViewById(R.id.cal);
        calendar.setCurrentDayBackgroundColor(ContextCompat.getColor(activity, R.color.colorCurrDay));
        calendar.setCurrentSelectedDayBackgroundColor(ContextCompat.getColor(activity, R.color.colorSelDay));
        calendar.shouldDrawIndicatorsBelowSelectedDays(true);

        calendar.setListener(new CompactCalendarView.CompactCalendarViewListener() {
            @Override
            public void onDayClick(Date dateClicked) {
                selectedDay.setTime(dateClicked);
                List<Event> events = calendar.getEvents(dateClicked);
                if (!events.isEmpty()) showEventsDialog(events);
            }

            @Override
            public void onMonthScroll(Date firstDayOfNewMonth) {
                selectedDay.setTime(firstDayOfNewMonth);
                loadCalendarData(firstDayOfNewMonth, true);
                setDateText(firstDayOfNewMonth);
            }
        });

        userFilter = (TextView) rootView.findViewById(R.id.userFilter);
        tvDate = (TextView) rootView.findViewById(R.id.tvDate);
        btnAll = (Button) rootView.findViewById(R.id.btnAll);
        btnAdd = (Button) rootView.findViewById(R.id.btnAdd);

        loaded = new ArrayList<>();
        allEvents = new ArrayList<>();
        lunchdateEvent = new HashMap<>();
        eventLunchdate = new HashMap<>();
        eventUser = new HashMap<>();

        return rootView;
    }

    @Override
    public void onResume() {
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

    private void setDateText(Date date){
        String dateString = monthyearFormat.format(date.getTime());

        // Parse Catalan month names to remove the initial preposition "de " or "d'"
        if (deviceLanguage.equals("ca")) dateString = dateString.replaceAll("^d[e]?\\W?", "");

        // Parse date string to force first letter to uppercase
        dateString = dateString.substring(0, 1).toUpperCase() + dateString.substring(1);

        tvDate.setText(dateString);
    }

    private void addCalendarEvents(List<Lunchdate> lunchdates) {
        // Create events and fill the corresponding Maps
        for (Lunchdate ld : lunchdates) {

            // Prevent duplicate keys if a lunchdate is added on a month not loaded yet
            if (!lunchdateEvent.containsKey(ld.getId())) {
                User user = activity.getUserById(ld.getUserId());

                // Prevent NullPointerException if an error has occurred at any time
                if (user != null) {
                    String data = user.toString();
                    if (ld.getStatus() != null) {
                        String status = (ld.getStatus().equals(Lunchdate.Event.ATTENDED.toString()))? getString(R.string.attended) : getString(R.string.no_show);
                        data += " - " + status;
                    }

                    Event evt = new Event(Color.BLACK, ld.getDate().getTime(), data);
                    allEvents.add(evt);
                    lunchdateEvent.put(ld.getId(), evt);
                    eventLunchdate.put(evt, ld);
                    eventUser.put(evt, user);
                }
            }
        }

        drawCalendarEvents();
    }

    private void addCalendarEvents(Lunchdate lunchdate) {
        List<Lunchdate> toAdd = new ArrayList<>();
        toAdd.add(lunchdate);
        addCalendarEvents(toAdd);
    }

    private void drawCalendarEvents() {
        calendar.removeAllEvents();

        // Draw all events or just the filtered user ones
        if (user == null) {
            calendar.addEvents(allEvents);
        } else {
            for (Event evt : eventUser.keySet()) {
                if (eventUser.get(evt).getId().equals(user.getId())) {
                    calendar.addEvent(evt, false);
                }
            }

            // Redraw calendar events
            calendar.invalidate();
        }
    }

    private void loadCalendarData(Date date, Boolean mainLoad) {
        Calendar calFrom = Calendar.getInstance();
        calFrom.setTime(date);
        calFrom.set(Calendar.DAY_OF_MONTH, calFrom.getActualMinimum(Calendar.DAY_OF_MONTH));
        calFrom.set(Calendar.HOUR_OF_DAY, 0);
        calFrom.set(Calendar.MINUTE, 0);
        calFrom.set(Calendar.SECOND, 0);
        calFrom.set(Calendar.MILLISECOND, 0);

        Calendar calTo = (Calendar) calFrom.clone();
        calTo.set(Calendar.DAY_OF_MONTH, calTo.getActualMaximum(Calendar.DAY_OF_MONTH));

        // Check if the data of the selected month have already been loaded
        if (!loaded.contains(calFrom.getTime())) {
            if (mainLoad) {
                showLoading();
            }

            loaded.add(calFrom.getTime());

            // Get lunchdates programmed for the selected month
            final List<Lunchdate> lunchdates = new ArrayList<>();

            db.collection("lunchdates")
                .whereEqualTo("account", account.getUid())
                .whereGreaterThanOrEqualTo("date", calFrom.getTime())
                .whereLessThanOrEqualTo("date", calTo.getTime())
                .get()
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(QuerySnapshot documentSnapshots) {
                        for (DocumentSnapshot doc : documentSnapshots.getDocuments()) {
                            Lunchdate ld = new Lunchdate(doc.getId(), doc.getString("account"), doc.getString("userId"), doc.getDate("date"), doc.getString("ref"), doc.getString("refText"), doc.getString("status"));
                            lunchdates.add(ld);
                        }
                        if (!lunchdates.isEmpty()) {
                            addCalendarEvents(lunchdates);
                        }
                        hideLoading();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(activity, getString(R.string.error_lunchdates_get), Toast.LENGTH_SHORT).show();
                        hideLoading();
                    }
                });
        }

        if (mainLoad) {
            // Load events from the previous & next months for a smooth change of months
            Calendar prev = (Calendar) calFrom.clone();
            prev.set(Calendar.MONTH, prev.get(Calendar.MONTH) - 1);

            Calendar next = (Calendar) calFrom.clone();
            next.set(Calendar.MONTH, next.get(Calendar.MONTH) + 1);

            loadCalendarData(prev.getTime(), false);
            loadCalendarData(next.getTime(), false);
        }
    }

    private String getRefText(Calendar from, Calendar to, Map<String, Integer> activeDows, Boolean alternate) {
        String res = "";

        //Create a list with the active dows names iterating dows array to preserve the order
        List<String> validDows = new ArrayList<>();
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
        res += " " + getString(R.string.from_lowercase) +
            " " + fulldateFormat.format(from.getTime()) +
            " " + getString(R.string.to_lowercase) +
            " " + fulldateFormat.format(to.getTime());

        //Concatenate if applies to alternate weeks
        if (alternate) res += " [" + getString(R.string.alternate_weeks) + "]";

        return res;
    }

    private boolean isBeforeOrEqual(Calendar cal1, Calendar cal2) {
        return (cal1.get(Calendar.YEAR) < cal2.get(Calendar.YEAR) ||
            (cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) <= cal2.get(Calendar.DAY_OF_YEAR)));
    }

    private boolean isValidWeek(Calendar from, Calendar cal) {
        // Check if a week is valid in the selected range with alternate-weeks option selected
        boolean res = false;
        int startWeek = from.get(Calendar.WEEK_OF_YEAR);
        int checkWeek = cal.get(Calendar.WEEK_OF_YEAR);
        //check if both current and received Calendar weeks are even or odd
        if (startWeek % 2 == checkWeek % 2) res = true;
        return res;
    }

    private void showEventsDialog(final List<Event> events) {
        // Sort Events list alphabetically
        Collections.sort(events, new Comparator<Event>(){
            @Override
            public int compare(final Event a, Event b) {
                return a.getData().toString().compareToIgnoreCase(b.getData().toString());
            }
        });

        final String[] evtStrings = new String[events.size()];
        for (int i=0; i<events.size(); i++){
            // Add trash icon before user name
            evtStrings[i] = "\uD83D\uDDD1  " + events.get(i).getData().toString();
        }

        String titleText = fulldateFormat.format(events.get(0).getTimeInMillis());
        if (user == null) titleText += " - " + getString(R.string.total) + ": " + events.size();

        TextView title = new TextView(activity);
        title.setText(titleText);
        title.setGravity(Gravity.CENTER);
        title.setPadding(0, 50, 0, 50);
        title.setTextSize(18);
        title.setTypeface(null, Typeface.BOLD);
        title.setTextColor(ContextCompat.getColor(activity, R.color.colorAccent));

        new AlertDialog.Builder(activity)
            .setCustomTitle(title)
            .setItems(evtStrings, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    showDelDialog(events.get(which));
                }
            })
            .setNegativeButton(getString(R.string.close), null)
            .show();
    }

    private void delLunchdate(final String lunchdateId, final Event event) {
        // Delete one lunchdate
        showLoading();

        db.collection("lunchdates").document(lunchdateId)
            .delete()
            .addOnSuccessListener(new OnSuccessListener<Void>() {
                @Override
                public void onSuccess(Void aVoid) {
                    Toast.makeText(activity, getString(R.string.lunchdate_deleted), Toast.LENGTH_SHORT).show();

                    Lunchdate lunchdate = eventLunchdate.get(event);

                    allEvents.remove(event);
                    lunchdateEvent.remove(lunchdateId);
                    eventLunchdate.remove(event);
                    eventUser.remove(event);
                    calendar.removeEvent(event, true);
                    hideLoading();

                    // Remove lunchdate from LunchTime tab list if it was scheduled for today

                    int day = Calendar.getInstance().get(Calendar.DAY_OF_YEAR);
                    int year = Calendar.getInstance().get(Calendar.YEAR);
                    Calendar lunchdateCal = Calendar.getInstance();
                    lunchdateCal.setTime(lunchdate.getDate());

                    if (lunchdateCal.get(Calendar.DAY_OF_YEAR) == day && lunchdateCal.get(Calendar.YEAR) == year) {
                        activity.delLunchdateFromLunchTime(lunchdate);
                    }
                }
            })
            .addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Toast.makeText(activity, getString(R.string.error_lunchdate_del), Toast.LENGTH_SHORT).show();
                    hideLoading();
                }
            });
    }

    private void delMultipleLunchdates(Query query) {
        // Delete multiple lunchdates
        showLoading();

        query
            .get()
            .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                @Override
                public void onComplete(@NonNull Task<QuerySnapshot> task) {
                    if (task.isSuccessful()) {
                        final List<String> lunchdateIds = new ArrayList<>();
                        WriteBatch batch = db.batch();

                        for (DocumentSnapshot doc : task.getResult()) {
                            String id = doc.getId();
                            lunchdateIds.add(id);
                            batch.delete(db.collection("lunchdates").document(id));
                        }

                        if (lunchdateIds.isEmpty()) {
                            hideLoading();
                        } else {
                            batch
                                .commit()
                                .addOnSuccessListener(new OnSuccessListener<Void>() {
                                    @Override
                                    public void onSuccess(Void aVoid) {
                                        Toast.makeText(activity, getString(R.string.lunchdates_deleted), Toast.LENGTH_SHORT).show();

                                        List<Event> eventsToRemove = new ArrayList<>();
                                        List<Lunchdate> todayLunchdates = new ArrayList<>();
                                        int day = Calendar.getInstance().get(Calendar.DAY_OF_YEAR);
                                        int year = Calendar.getInstance().get(Calendar.YEAR);

                                        // Remove events from calendar, lists & maps if were drawn & check if any was scheduled for today
                                        for (String id : lunchdateIds) {
                                            if (lunchdateEvent.containsKey(id)) {
                                                Event event = lunchdateEvent.get(id);
                                                eventsToRemove.add(event);

                                                Lunchdate lunchdate = eventLunchdate.get(event);

                                                Calendar lunchdateCal = Calendar.getInstance();
                                                lunchdateCal.setTime(lunchdate.getDate());

                                                if (lunchdateCal.get(Calendar.DAY_OF_YEAR) == day && lunchdateCal.get(Calendar.YEAR) == year) {
                                                    todayLunchdates.add(lunchdate);
                                                }
                                            }
                                        }

                                        allEvents.removeAll(eventsToRemove);
                                        lunchdateEvent.keySet().removeAll(lunchdateIds);
                                        eventLunchdate.keySet().removeAll(eventsToRemove);
                                        eventUser.keySet().removeAll(eventsToRemove);
                                        calendar.removeEvents(eventsToRemove);

                                        hideLoading();

                                        // Remove from LunchTime tab list the lunchdates scheduled for today
                                        if (!todayLunchdates.isEmpty()) {
                                            activity.delLunchdatesFromLunchTime(todayLunchdates);
                                        }
                                    }
                                })
                                .addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        Toast.makeText(activity, getString(R.string.error_lunchdates_del), Toast.LENGTH_SHORT).show();
                                        hideLoading();
                                    }
                                });
                        }
                    } else {
                        Toast.makeText(activity, getString(R.string.error_lunchdates_del), Toast.LENGTH_SHORT).show();
                        hideLoading();
                    }
                }
            });
    }

    private void delRange(String uid, String ref) {
        // Delete a range of lunchdates by the ref token
        Query query = db.collection("lunchdates")
            .whereEqualTo("account", account.getUid())
            .whereEqualTo("userId", uid)
            .whereEqualTo("ref", ref);
        delMultipleLunchdates(query);
    }

    private void showDelDialog(final Event event) {
        final User user = eventUser.get(event);
        final Lunchdate ld = eventLunchdate.get(event);

        // Show dialog depending on lunchdate type: range or single day
        if (ld.getRef() != null && !ld.getRef().isEmpty()) {
            // Range of dates
            // User can delete just this date or the full range

            String[] options = {
                getString(R.string.del_single_day),
                getString(R.string.del_all_dates_range) + ":\n" + ld.getRefText()
            };

            new AlertDialog.Builder(activity)
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
                            //Delete all lunchdates with the same ref token
                            delRange(user.getId(), ld.getRef());

                            // Reset delAll variable to false
                            delAll = false;
                        } else {
                            // Delete the selected lunchdate
                            delLunchdate(ld.getId(), event);
                        }
                    }
                })
                .setNegativeButton(getString(R.string.cancel), null)
                .show();
        } else {
            // Single day

            new AlertDialog.Builder(activity)
                .setCancelable(true)
                .setTitle(fulldateFormat.format(event.getTimeInMillis()) + " - " + user.toString())
                .setMessage(getString(R.string.del_lunchdate) + ". " + getString(R.string.sure))
                .setPositiveButton(getString(R.string.confirm), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        delLunchdate(ld.getId(), event);
                    }
                })
                .setNegativeButton(getString(R.string.cancel), null)
                .show();
        }
    }

    private void showLoading() {
        loading.setVisibility(View.VISIBLE);
    }

    private void hideLoading() {
        loading.setVisibility(View.INVISIBLE);
    }

    public void init() {
        Date now = new Date();
        loadCalendarData(now, true);
        setDateText(now);
    }

    public void onEnter() {
        delFilter();
        selectedDay.setTime(new Date());
    }

    public void showUsersListDialog() {
        usersListDialog = new UsersListDialog(activity, activity.allUsers);
        usersListDialog.show();
    }

    public void closeUsersListDialog() {
        if (usersListDialog != null && usersListDialog.isShowing()) {
            usersListDialog.dismiss();
        }
    }

    public void setFilter(User u) {
        user = u;
        userFilter.setText(user.toString());
        btnAll.setVisibility(View.VISIBLE);
        btnAdd.setEnabled(true);
        drawCalendarEvents();
        closeUsersListDialog();
    }

    public void delFilter() {
        // Delete user filter if existed
        if (user != null) {
            user = null;
            userFilter.setText(getString(R.string.all_users));
            btnAll.setVisibility(View.INVISIBLE);
            btnAdd.setEnabled(false);
            drawCalendarEvents();
            closeUsersListDialog();
        }
    }

    public void showLunchdateForm() {
        lunchdateFormDialog = new LunchdateFormDialog(activity);
        lunchdateFormDialog.show();
        lunchdateFormDialog.setData(user, selectedDay.getTime());
    }

    public void closeLunchdateForm() {
        lunchdateFormDialog.dismiss();
    }

    public void saveLunchdate() {
        // Prevent double click on save button
        lunchdateFormDialog.disableButtons();

        if (lunchdateFormDialog.isRange()) {
            // Range of dates

            Calendar from = lunchdateFormDialog.getFrom();
            Calendar to = lunchdateFormDialog.getTo();
            Map<String, Integer> activeDows = lunchdateFormDialog.getActiveDows();

            final List<Lunchdate> lunchdates = new ArrayList<>();
            String ref = new Token().toString();
            String refText = getRefText(from, to, activeDows, lunchdateFormDialog.isAlternateWeeks());

            Calendar cal = (Calendar) from.clone();

            for (Date date = cal.getTime(); isBeforeOrEqual(cal, to); cal.add(Calendar.DATE, 1), date = cal.getTime()) {
                // Loop from start to end of the range selecting each valid day of the week of each valid week
                if (!lunchdateFormDialog.isAlternateWeeks() || isValidWeek(from, cal)) {
                    int dow = cal.get(Calendar.DAY_OF_WEEK) - 2; // it returns 2 for Monday & we want it to be 0 like the dows array index
                    if (dow == -1) dow = 6;  // Sunday was originally 1 & we want it to be 6
                    if (activeDows.get(dows[dow]) == 1) lunchdates.add(new Lunchdate(account.getUid(), user.getId(), date, ref, refText));
                }
            }

            lunchdateFormDialog.enableButtons();

            if (lunchdates.isEmpty()) {
                Toast.makeText(activity, getString(R.string.dates_not_valid), Toast.LENGTH_SHORT).show();
            } else {
                closeLunchdateForm();
                showLoading();

                WriteBatch batch = db.batch();

                final List<Lunchdate> todayLunchdates = new ArrayList<>();
                int day = Calendar.getInstance().get(Calendar.DAY_OF_YEAR);
                int year = Calendar.getInstance().get(Calendar.YEAR);

                for (Lunchdate lunchdate : lunchdates) {
                    DocumentReference docRef = db.collection("lunchdates").document();
                    lunchdate.setId(docRef.getId());
                    batch.set(docRef, lunchdate);

                    Calendar lunchdateCal = Calendar.getInstance();
                    lunchdateCal.setTime(lunchdate.getDate());
                    if (lunchdateCal.get(Calendar.DAY_OF_YEAR) == day && lunchdateCal.get(Calendar.YEAR) == year) {
                        todayLunchdates.add(lunchdate);
                    }
                }

                batch
                    .commit()
                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            Toast.makeText(activity, getString(R.string.lunchdates_added), Toast.LENGTH_SHORT).show();
                            addCalendarEvents(lunchdates);
                            hideLoading();

                            // Add to LunchTime tab list the lunchdates scheduled for today
                            if (!todayLunchdates.isEmpty()) {
                                activity.addLunchdatesToLunchTime(todayLunchdates);
                            }
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Toast.makeText(activity, getString(R.string.error_lunchdate_add), Toast.LENGTH_SHORT).show();
                            hideLoading();
                        }
                    });
            }
        } else {
            // Single day
            closeLunchdateForm();
            showLoading();

            DocumentReference docRef = db.collection("lunchdates").document();
            final Lunchdate lunchdate = new Lunchdate(docRef.getId(), account.getUid(), user.getId(), lunchdateFormDialog.getFrom().getTime());

            docRef
                .set(lunchdate)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Toast.makeText(activity, getString(R.string.lunchdate_added), Toast.LENGTH_SHORT).show();

                        addCalendarEvents(lunchdate);
                        hideLoading();

                        // Add lunchdate to LunchTime tab list if it was scheduled for today

                        int day = Calendar.getInstance().get(Calendar.DAY_OF_YEAR);
                        int year = Calendar.getInstance().get(Calendar.YEAR);
                        Calendar lunchdateCal = Calendar.getInstance();
                        lunchdateCal.setTime(lunchdate.getDate());

                        if (lunchdateCal.get(Calendar.DAY_OF_YEAR) == day && lunchdateCal.get(Calendar.YEAR) == year) {
                            activity.addLunchdateToLunchTime(lunchdate);
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(activity, getString(R.string.error_lunchdate_add), Toast.LENGTH_SHORT).show();
                        hideLoading();
                    }
                });
        }
    }

    public void delLunchdateByUser(String uid) {
        // Delete all Lunchdates of an user
        Query query = db.collection("lunchdates")
                        .whereEqualTo("account", account.getUid())
                        .whereEqualTo("userId", uid);
        delMultipleLunchdates(query);
    }

    public void updateLunchdateStatus(Lunchdate ld) {
        // necesito actualisar el evento para que ponga si ha asistido y eso


        Event event = lunchdateEvent.get(ld.getId());
        User u = activity.getUserById(ld.getUserId());

        // Remove Event & its references
        allEvents.remove(event);
        lunchdateEvent.remove(ld.getId());
        eventLunchdate.remove(event);
        eventUser.remove(event);
        calendar.removeEvent(event, true);

        String data = u.toString();
        if (ld.getStatus() != null) {
            String status = (ld.getStatus().equals(Lunchdate.Event.ATTENDED.toString()))? getString(R.string.attended) : getString(R.string.no_show);
            data += " - " + status;
        }

        // Create new Event and put in the corresponding Collections
        Event newEvent = new Event(Color.BLACK, ld.getDate().getTime(), data);
        allEvents.add(newEvent);
        lunchdateEvent.put(ld.getId(), newEvent);
        eventLunchdate.put(newEvent, ld);
        eventUser.put(newEvent, user);
        calendar.addEvent(newEvent, false);
    }
}
