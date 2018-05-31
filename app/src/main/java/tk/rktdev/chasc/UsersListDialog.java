package tk.rktdev.chasc;

import android.app.Dialog;
import android.os.Bundle;
import android.view.View;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class UsersListDialog extends Dialog {
    public TabsActivity activity;
    private List<User> allUsers, usersList;
    private UsersListDialogListAdapter listAdapter;
    private ListView lv;
    private TextView tvNoUsers;
    private SearchView sv;
    private String searchString = "";

    public UsersListDialog(TabsActivity a, List<User> users) {
        super(a);
        this.activity = a;
        this.allUsers = new ArrayList<>(users);
        this.usersList = new ArrayList<>(users);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_users_list);

        tvNoUsers = (TextView) findViewById(R.id.tvNoUsers);
        lv = (ListView) findViewById(R.id.list);
        listAdapter = new UsersListDialogListAdapter(usersList, activity);
        lv.setAdapter(listAdapter);
        filterUsers();

        sv = (SearchView) findViewById(R.id.sv);
        sv.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                searchString = newText;
                filterUsers();
                return false;
            }
        });
    }

    private void filterUsers() {
        usersList.clear();

        for (User u : allUsers) {
            if (searchString.equals("") ||
                u.getName().toLowerCase().contains(searchString.toLowerCase()) ||
                u.getSurname().toLowerCase().contains(searchString.toLowerCase())) usersList.add(u);
        }

        if (usersList.isEmpty()) {
            lv.setVisibility(View.INVISIBLE);
            tvNoUsers.setVisibility(View.VISIBLE);
            if (searchString.isEmpty()) tvNoUsers.setText(activity.getString(R.string.no_users));
            else tvNoUsers.setText(activity.getString(R.string.no_results));
        } else {
            tvNoUsers.setVisibility(View.INVISIBLE);
            listAdapter.updateList(usersList);
            lv.setAdapter(listAdapter);
            lv.setVisibility(View.VISIBLE);
        }
    }
}
