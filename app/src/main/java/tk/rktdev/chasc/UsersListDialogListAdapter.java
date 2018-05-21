package tk.rktdev.chasc;


import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.List;

// ListAdapter to customize the admin users list
public class UsersListDialogListAdapter extends BaseAdapter {
    private List<User> list;
    private TabsActivity activity;

    public UsersListDialogListAdapter(List<User> l, TabsActivity a) {
        list = l;
        activity = a;
    }

    @Override
    public int getCount() {
        return list.size();
    }

    @Override
    public Object getItem(int pos) {
        return list.get(pos);
    }

    @Override
    public long getItemId(int pos) {
        return pos;
    }

    @Override
    public View getView(final int position, View view, ViewGroup parent) {
        if (view == null) {
            LayoutInflater inflater = (LayoutInflater) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            view = inflater.inflate(R.layout.listadapter_users_dialog, null);
        }

        TextView listItemText = view.findViewById(R.id.listItemString);
        listItemText.setText(list.get(position).toString());

        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                activity.setCalendarFilter(list.get(position).getId());
            }
        });

        return view;
    }

    public void updateList(List<User> newList) {
        list = newList;
    }
}
