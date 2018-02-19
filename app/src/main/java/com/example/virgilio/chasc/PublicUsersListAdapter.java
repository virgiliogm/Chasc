package com.example.virgilio.chasc;


import android.content.Context;
import android.support.v4.content.ContextCompat;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.ListAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

// ListAdapter to customize the public users list
public class PublicUsersListAdapter extends BaseAdapter implements ListAdapter {
    private DBUtil db;
    private Context context;
    private Spinner spAction;
    private String[] actions;
    private List<User> users;
    private List<Schedule> schedules;
    private Map<Schedule.Event, Integer> colorsByEvent;

    // Map where link each item position to its spinner to be able to open it dynamically
    private SparseArray<Spinner> spinners;

    public PublicUsersListAdapter(List<User> users, List<Schedule> schedules, Context context) {
        this.users = users;
        this.schedules = schedules;
        this.context = context;

        //Fill actions array with the translated strings
        actions = new String[] {
            context.getResources().getString(R.string.select_action),
            context.getResources().getString(R.string.attended),
            context.getResources().getString(R.string.no_show),
            context.getResources().getString(R.string.cancel)
        };

        spinners = new SparseArray<>();

        // Link Schedule Events to a color to highlight the list rows, including null reference for schedules with no event
        colorsByEvent = new HashMap<>();
        colorsByEvent.put(Schedule.Event.ATTENDED, ContextCompat.getColor(context, R.color.colorSuccess));
        colorsByEvent.put(Schedule.Event.NO_SHOW, ContextCompat.getColor(context, R.color.colorDanger));
        colorsByEvent.put(null, 0);

        db = new DBUtil();
    }

    @Override
    public int getCount() {
        return users.size();
    }

    @Override
    public Object getItem(int pos) {
        return users.get(pos);
    }

    @Override
    public long getItemId(int pos) {
        return users.get(pos).getId();
    }

    @Override
    public View getView(final int position, View view, ViewGroup parent) {
        if (view == null) {
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            view = inflater.inflate(R.layout.public_users_list_adapter, null);
        }

        User user = users.get(position);
        final Schedule schedule = schedules.get(position);
        final View currentView = view;
        currentView.setBackgroundColor(colorsByEvent.get(schedule.getEvent()));

        TextView listItem = currentView.findViewById(R.id.listItemString);
        if (listItem != null) listItem.setText(user.toString());

        spAction = currentView.findViewById(R.id.spAction);
        spinners.put(position, spAction);
        spAction.setAdapter(new ArrayAdapter<String>(context, android.R.layout.simple_spinner_dropdown_item, actions){
            @Override
            public View getDropDownView(int position, View convertView, ViewGroup parent) {
                View mView = super.getDropDownView(position, convertView, parent);

                //Disable the first item of the spinner: "Select action"
                if (position == 0) mView.setEnabled(false);

                return mView;
            }
        });

        // Open Spinner on click anywhere on the current list row
        currentView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                spinners.get(position).performClick();
            }
        });

        spAction.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int idx, long id) {
                if (idx == 1) {
                    // Attended
                    tick(schedule, Schedule.Event.ATTENDED, position, currentView);
                } else if (idx == 2) {
                    // No show
                    tick(schedule, Schedule.Event.NO_SHOW, position, currentView);
                } else if (idx == 3) {
                    // Cancel
                    tick(schedule, null, position, currentView);
                }

                // Unselect option in Spinner
                spAction.setSelection(0);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) { }
        });

        return view;
    }

    private void tick(Schedule sch, Schedule.Event evt, int pos, View view) {
        // Mark a Schedule with the corresponding Event

        if (db.scheduleTick(sch, evt)) {
            sch.setEvent(evt);
            schedules.set(pos, sch);
            view.setBackgroundColor(colorsByEvent.get(evt));
        }
    }
}
