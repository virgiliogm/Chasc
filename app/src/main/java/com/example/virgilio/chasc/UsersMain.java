package com.example.virgilio.chasc;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.TextView;

import java.util.List;

public class UsersMain extends AppCompatActivity {
    private DBUtil db;
    private SearchView sv;
    private TextView tvNoUsers;
    private ListView lv1;
    private String searchString = "";
    private List<User> usersList;

    private BroadcastReceiver delReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_users_main);

        // Show back button on top bar
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        db = new DBUtil();

        lv1 = (ListView) findViewById(R.id.list);
        tvNoUsers = (TextView) findViewById(R.id.tvNoUsers);

        sv = (SearchView) findViewById(R.id.sv);
        sv.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                searchString = newText;
                loadUsers();
                return false;
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadUsers();

        // Listen to "user.del" event emmited by AdminUsersListAdapter when a user is deleted
        delReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                loadUsers();
            }
        };
        registerReceiver(delReceiver , new IntentFilter("user.del"));
    }

    @Override
    protected void onPause() {
        super.onPause();

        // Stop listening to "user.del" event
        unregisterReceiver(delReceiver);
    }

    // Finish activity on top bar back button click
    public boolean onOptionsItemSelected(MenuItem item){
        finish();
        return true;
    }

    private void loadUsers() {
        usersList = db.usersGet(searchString);
        if (usersList == null || usersList.isEmpty()) {
            lv1.setVisibility(View.INVISIBLE);
            tvNoUsers.setVisibility(View.VISIBLE);
            if (searchString.isEmpty()) tvNoUsers.setText(getString(R.string.no_users));
            else tvNoUsers.setText(getString(R.string.no_results));
        } else {
            tvNoUsers.setVisibility(View.INVISIBLE);
            lv1.setVisibility(View.VISIBLE);
            lv1.setAdapter(new AdminUsersListAdapter(usersList, this));
        }
    }

    public void add (View v){
        Intent i = new Intent(this, UsersOneForm.class);
        startActivity(i);
    }

    public void calendar (View v){
        Intent i = new Intent(this, UsersScheduleMain.class);
        startActivity(i);
    }
}
