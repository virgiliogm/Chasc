package tk.rktdev.chasc;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class TabLunchTime extends Fragment {
    private FirebaseFirestore db;
    private FirebaseUser account;

    private Calendar today;

    private TabsActivity activity;
    private List<Lunchdate> lunchdates;
    private List<User> todayUsers;
    private TabLunchTimeListAdapter listAdapter;

    private RelativeLayout loading;
    private SearchView sv;
    private TextView tvNoUsers;
    private ListView list;
    private String searchString = "";


    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.tab_lunchtime, container, false);
        activity = (TabsActivity) getActivity();

        db = activity.getDb();
        account = activity.getAccount();

        loading = (RelativeLayout) rootView.findViewById(R.id.loading);

        today = Calendar.getInstance();

        lunchdates = new ArrayList<>();
        todayUsers = new ArrayList<>();
        listAdapter = new TabLunchTimeListAdapter(todayUsers, lunchdates, activity);

        list = (ListView) rootView.findViewById(R.id.list);
        tvNoUsers = (TextView) rootView.findViewById(R.id.tvNoUsers);

        sv = (SearchView) rootView.findViewById(R.id.sv);
        sv.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                searchString = newText;
                filterList();
                return false;
            }
        });

        return rootView;
    }

    private void loadLunchdates() {
        showLoading();

        Calendar today = Calendar.getInstance();
        today.set(Calendar.HOUR_OF_DAY, 0);
        today.set(Calendar.MINUTE, 0);
        today.set(Calendar.SECOND, 0);
        today.set(Calendar.MILLISECOND, 0);

        lunchdates.clear();
        todayUsers.clear();

        db.collection("lunchdates")
            .whereEqualTo("account", account.getUid())
            .whereEqualTo("date", today.getTime())
            .get()
            .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                @Override
                public void onComplete(@NonNull Task<QuerySnapshot> task) {
                    // Prevent Fragment not attached to Activity error after logout
                    if (isAdded()) {
                        if (task.isSuccessful()) {
                            // Parse the results and fill the corresponding lists
                            for (DocumentSnapshot doc : task.getResult().getDocuments()) {
                                Lunchdate ld = new Lunchdate(doc.getId(), doc.getString("account"), doc.getString("userId"), doc.getDate("date"), doc.getString("ref"), doc.getString("refText"), doc.getString("status"));
                                lunchdates.add(ld);
                                todayUsers.add(activity.getUserById(doc.getString("userId")));
                            }
                            sortLists();
                            filterList();

                            hideLoading();
                        } else {
                            Toast.makeText(activity, getString(R.string.error_users_get), Toast.LENGTH_SHORT).show();
                            hideLoading();
                        }
                    }
                }
            });
    }

    private void filterList() {
        if (todayUsers.isEmpty()) {
            list.setVisibility(View.INVISIBLE);
            tvNoUsers.setVisibility(View.VISIBLE);
            if (searchString.isEmpty()) tvNoUsers.setText(getString(R.string.no_users));
            else tvNoUsers.setText(getString(R.string.no_results));
        } else {
            // Fill the users & lunchdates lists filtered by the search string
            List<User> filteredUsers = new ArrayList<>();
            List<Lunchdate> filteredLunchdates = new ArrayList<>();

            for (int i=0; i<todayUsers.size(); i++) {
                if (todayUsers.get(i).getName().toLowerCase().contains(searchString.toLowerCase()) ||
                    todayUsers.get(i).getSurname().toLowerCase().contains(searchString.toLowerCase())) {
                    filteredUsers.add(todayUsers.get(i));
                    filteredLunchdates.add(lunchdates.get(i));
                }
            }

            tvNoUsers.setVisibility(View.INVISIBLE);
            listAdapter.updateList(filteredUsers, filteredLunchdates);
            list.setAdapter(listAdapter);
            list.setVisibility(View.VISIBLE);
        }
    }

    private void sortLists() {
        // Sort users list & then sort lunchdates list by the same order
        activity.sortUsersList(todayUsers);

        final List<String> userIds = new ArrayList<>();
        for (User user : todayUsers) {
            userIds.add(user.getId());
        }

        Collections.sort(lunchdates, new Comparator<Lunchdate>(){
            @Override
            public int compare(Lunchdate a, Lunchdate b) {
                Integer aIndex = (Integer) userIds.indexOf(a.getUserId());
                Integer bIndex = (Integer) userIds.indexOf(b.getUserId());
                return aIndex.compareTo(bIndex);
            }
        });
    }

    public void showLoading() {
        loading.setVisibility(View.VISIBLE);
    }

    public void hideLoading() {
        loading.setVisibility(View.INVISIBLE);
    }

    public void init() {
        if (!activity.allUsers.isEmpty()) {
            loadLunchdates();
        }
    }

    public void onEnter() {
        sv.setQuery("", false);
        sv.setIconified(true);
        if (todayUsers.isEmpty()) {
            list.setVisibility(View.INVISIBLE);
            tvNoUsers.setVisibility(View.VISIBLE);
            tvNoUsers.setText(getString(R.string.no_users));
        } else {
            tvNoUsers.setVisibility(View.INVISIBLE);
            listAdapter.updateList(todayUsers, lunchdates);
            list.setAdapter(listAdapter);
            list.setVisibility(View.VISIBLE);
        }

        // If entering the tab on a different day than last time, reload the lunchdates list
        Calendar cal = Calendar.getInstance();
        if (cal.get(Calendar.DAY_OF_YEAR) != today.get(Calendar.DAY_OF_YEAR) || cal.get(Calendar.YEAR) != today.get(Calendar.YEAR)) {
            today.setTime(cal.getTime());
            loadLunchdates();
        }
    }

    public void nfcRead(String nfcId) {
        // On NFC read: check if NFC ID belongs to an authorized user and show Dialog
        User user = null;

        for (User u : activity.allUsers) {
            if (u.getNfc().equals(nfcId)) {
                user = u;
            }
        }

        if (user == null) {
            // If no user is linked to this NFC device
            showAuthDialog(user, false);
        } else {
            // Check if user is in lunchdates list
            final int index = todayUsers.indexOf(user);
            if (index != -1) {
                showLoading();

                // Lunchdate index in lunchdates is the same than User index in todayUsers
                final Lunchdate lunchdate = lunchdates.get(index);
                lunchdate.setStatus(Lunchdate.Event.ATTENDED.toString());

                // Mark it as attended in database and update List
                final User finalUser = user;
                db.collection("lunchdates")
                    .document(lunchdate.getId())
                    .set(lunchdate)
                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            // Prevent Fragment not attached to Activity error after logout
                            if (isAdded()) {
                                if (task.isSuccessful()) {
                                    lunchdates.set(index, lunchdate);
                                    listAdapter.updateList(todayUsers, lunchdates);
                                    list.setAdapter(listAdapter);
                                    showAuthDialog(finalUser, true);
                                    activity.updateLunchdateStatus(lunchdate);
                                    hideLoading();
                                } else {
                                    Toast.makeText(activity, getString(R.string.error_lunchdate_mod), Toast.LENGTH_SHORT).show();
                                    hideLoading();
                                }
                            }
                        }
                    });
            } else {
                showAuthDialog(user, false);
            }
        }
    }

    public void showAuthDialog(User user, boolean auth) {
        // Show Dialog

        String title = (auth)? getString(R.string.auth_user) : getString(R.string.noauth_user);
        String msg = (user == null)? getString(R.string.no_user_nfc) : user.toString();
        int bgcolor = (auth)? R.color.colorSuccess : R.color.colorDanger;

        AlertDialog.Builder dialog = new AlertDialog.Builder(activity).setTitle(title).setMessage(msg);

        final AlertDialog alert = dialog.show();
        alert.getWindow().setBackgroundDrawableResource(bgcolor);

        // Hide dialog after 2 seconds
        final Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            public void run() {
                alert.dismiss();
                timer.cancel();
            }
        }, 2000);
    }

    public void addLunchdate(Lunchdate lunchdate) {
        lunchdates.add(lunchdate);
        todayUsers.add(activity.getUserById(lunchdate.getUserId()));
        sortLists();
    }

    public void addLunchdates(List<Lunchdate> lunchdates) {
        for (Lunchdate lunchdate : lunchdates) {
            lunchdates.add(lunchdate);
            todayUsers.add(activity.getUserById(lunchdate.getUserId()));
        }
        sortLists();
    }

    public void delLunchdate(Lunchdate lunchdate) {
        if (lunchdates.contains(lunchdate)) {
            lunchdates.remove(lunchdate);
        }
        for (int i = todayUsers.size()-1; i >= 0; i--) {
            User user = todayUsers.get(i);
            if (user.getId().equals(lunchdate.getUserId())) {
                todayUsers.remove(user);
            }
        }
        sortLists();
        listAdapter.updateList(todayUsers, lunchdates);
    }

    public void delLunchdates(List<Lunchdate> lunchdates) {
        for (int i = lunchdates.size()-1; i >= 0; i--) {
            Lunchdate lunchdate = lunchdates.get(i);
            if (lunchdates.contains(lunchdate)) {
                lunchdates.remove(lunchdate);
            }
            for (int j = todayUsers.size()-1; j >= 0; j--) {
                User user = todayUsers.get(j);
                if (user.getId().equals(lunchdate.getUserId())) {
                    todayUsers.remove(user);
                }
            }
        }
        sortLists();
        listAdapter.updateList(todayUsers, lunchdates);
    }
}
