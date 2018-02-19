package com.example.virgilio.chasc;


import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
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

import java.util.List;

// ListAdapter to customize the admin users list
public class AdminUsersListAdapter extends BaseAdapter implements ListAdapter {
    private DBUtil db;
    private List<User> list;
    private Context context;
    private Spinner spAction;
    private String[] actions;

    // Map where link each item position to its spinner to be able to open it dynamically
    private SparseArray<Spinner> spinners;


    public AdminUsersListAdapter(List<User> list, Context context) {
        this.list = list;
        this.context = context;

        //Fill actions array with the translated strings
        actions = new String[] {
            context.getResources().getString(R.string.select_action),
            context.getResources().getString(R.string.edit),
            context.getResources().getString(R.string.calendar),
            context.getResources().getString(R.string.nfc),
            context.getResources().getString(R.string.delete)
        };

        spinners = new SparseArray<>();
        db = new DBUtil();
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
        return list.get(pos).getId();
    }

    @Override
    public View getView(final int position, View view, ViewGroup parent) {
        if (view == null) {
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            view = inflater.inflate(R.layout.admin_users_list_adapter, null);
        }

        TextView listItemText = view.findViewById(R.id.listItemString);
        listItemText.setText(list.get(position).toString());

        spAction = view.findViewById(R.id.spAction);
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
        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                spinners.get(position).performClick();
            }
        });

        spAction.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int idx, long id) {
                Intent intent;
                Bundle b;

                if (idx == 1) {
                    // Edit
                    intent = new Intent(context, UsersOneForm.class);
                    b = new Bundle();
                    b.putInt("id", list.get(position).getId());
                    intent.putExtras(b);
                    context.startActivity(intent);
                } else if (idx == 2) {
                    // Calendar
                    intent = new Intent(context, UsersOneScheduleMain.class);
                    b = new Bundle();
                    b.putInt("id", list.get(position).getId());
                    intent.putExtras(b);
                    context.startActivity(intent);
                } else if (idx == 3) {
                    // NFC
                    intent = new Intent(context, UsersOneNfcForm.class);
                    b = new Bundle();
                    b.putInt("id", list.get(position).getId());
                    intent.putExtras(b);
                    context.startActivity(intent);
                } else if (idx == 4) {
                    // Delete
                    showConfirmDialog(position);
                }

                // Unselect option in Spinner
                spAction.setSelection(0);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) { }
        });

        return view;
    }

    private void showConfirmDialog(final int pos) {
        final User user = list.get(pos);
        new AlertDialog.Builder(context)
            .setTitle(user.toString())
            .setMessage(context.getResources().getString(R.string.del_user) + ". " + context.getResources().getString(R.string.sure))
            .setPositiveButton(context.getResources().getString(R.string.confirm), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                if (db.userDel(user)) {
                    //Broadcast event that UsersMain is listening to reload users list
                    Intent broadcast = new Intent();
                    broadcast.setAction("user.del");
                    context.sendBroadcast(broadcast);
                }
                }
            })
            .setNegativeButton(context.getResources().getString(R.string.cancel), null)
            .show();
    }
}
