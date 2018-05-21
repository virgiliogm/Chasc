package tk.rktdev.chasc;


import android.content.Context;
import android.graphics.Typeface;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.TextView;

import java.util.List;

// ListAdapter to customize the admin users users
public class TabUsersListAdapter extends BaseAdapter {
    private List<User> users;
    private TabsActivity activity;
    private Button btnOptions;


    public TabUsersListAdapter(List<User> l, TabsActivity a) {
        users = l;
        activity = a;
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
        return pos;
    }

    @Override
    public View getView(final int position, View view, ViewGroup parent) {
        if (view == null) {
            LayoutInflater inflater = (LayoutInflater) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            view = inflater.inflate(R.layout.listadapter_tab_users, null);
        }

        final User user = users.get(position);
        TextView listItem = view.findViewById(R.id.listItemString);
        if (listItem != null) listItem.setText(user.toString());


        // Show dialog by clicking on the button or anywhere in the current row of the list
        btnOptions = (Button) view.findViewById(R.id.btnOptions);
        btnOptions.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDialog(user);
            }
        });
        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDialog(user);
            }
        });

        return view;
    }

    private void showDialog(User user) {
        final String uid = user.getId();

        TextView title = new TextView(activity);
        title.setText(user.toString());
        title.setGravity(Gravity.CENTER);
        title.setPadding(0, 50, 0, 50);
        title.setTextSize(18);
        title.setTypeface(null, Typeface.BOLD);
        title.setTextColor(ContextCompat.getColor(activity, R.color.colorAccent));

        final AlertDialog dialog = new AlertDialog.Builder(activity)
            .setView(R.layout.dialog_user_options)
            .setCustomTitle(title)
            .setNegativeButton(R.string.close, null)
            .create();

        dialog.show();

        View.OnClickListener listener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (v.getId() == R.id.btnEdit) {
                    activity.editUser(uid);
                } else if (v.getId() == R.id.btnCalendar) {
                    activity.switchToTab("calendar");
                    activity.setCalendarFilter(uid);
                } else if (v.getId() == R.id.btnDelete) {
                    activity.delUser(uid);
                }
                dialog.dismiss();
            }
        };

        dialog.findViewById(R.id.btnEdit).setOnClickListener(listener);
        dialog.findViewById(R.id.btnCalendar).setOnClickListener(listener);
        dialog.findViewById(R.id.btnDelete).setOnClickListener(listener);
    }

    public void updateList(List<User> l) {
        users = l;
    }

    public void delUser(int pos) {
        users.remove(pos);
    }
}
