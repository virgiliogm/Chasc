package tk.rktdev.chasc;

import android.content.DialogInterface;
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
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class TabUsers extends Fragment {
    private FirebaseFirestore db;
    private FirebaseUser account;

    private TabsActivity activity;
    private UserFormDialog userFormDialog;
    private User user;
    private List<User> filteredUsers;
    private TabUsersListAdapter listAdapter;

    private RelativeLayout loading;
    private ListView lv;
    private TextView tvNoUsers;
    private SearchView sv;
    private String searchString = "";


    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        final View rootView = inflater.inflate(R.layout.tab_users, container, false);
        activity = (TabsActivity) getActivity();

        db = activity.getDb();
        account = activity.getAccount();

        loading = (RelativeLayout) rootView.findViewById(R.id.loading);

        filteredUsers = new ArrayList<>();
        lv = (ListView) rootView.findViewById(R.id.list);
        listAdapter = new TabUsersListAdapter(filteredUsers, activity);

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

        loadUsers();

        return rootView;
    }

    private void loadUsers() {
        showLoading();

        activity.allUsers.clear();
        sv.setQuery("", false);
        sv.setIconified(true);

        db.collection("users")
            .whereEqualTo("account", account.getUid())
            .get()
            .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                @Override
                public void onComplete(@NonNull Task<QuerySnapshot> task) {
                    // Prevent Fragment not attached to Activity error after logout
                    if (isAdded()) {
                        if (task.isSuccessful()) {
                            for (DocumentSnapshot doc : task.getResult().getDocuments()) {
                                User u = new User(doc.getId(), doc.getString("account"), doc.getString("name"), doc.getString("surname"), doc.getString("nfc"));
                                activity.allUsers.add(u);
                            }

                            sortUsersList(activity.allUsers);

                            // Once users are loaded, init Calendar & LunchTime tabs
                            activity.initCalendar();
                            activity.initLunchTime();

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
        filteredUsers.clear();

        for (User u : activity.allUsers) {
            if (searchString.equals("") ||
                u.getName().toLowerCase().contains(searchString.toLowerCase()) ||
                u.getSurname().toLowerCase().contains(searchString.toLowerCase())) filteredUsers.add(u);
        }

        if (filteredUsers.isEmpty()) {
            lv.setVisibility(View.INVISIBLE);
            tvNoUsers.setVisibility(View.VISIBLE);
            if (searchString.isEmpty()) tvNoUsers.setText(getString(R.string.no_users));
            else tvNoUsers.setText(getString(R.string.no_results));
        } else {
            tvNoUsers.setVisibility(View.INVISIBLE);
            listAdapter.updateList(filteredUsers);
            lv.setAdapter(listAdapter);
            lv.setVisibility(View.VISIBLE);
        }
    }

    private void updateList() {
        sortUsersList(activity.allUsers);
        filterList();
    }

    private void showLoading() {
        loading.setVisibility(View.VISIBLE);
    }

    private void hideLoading() {
        loading.setVisibility(View.INVISIBLE);
    }

    public void onEnter() {
        sv.setQuery("", false);
        sv.setIconified(true);
        if (activity.allUsers.isEmpty()) {
            lv.setVisibility(View.INVISIBLE);
            tvNoUsers.setVisibility(View.VISIBLE);
            tvNoUsers.setText(getString(R.string.no_users));
        } else {
            tvNoUsers.setVisibility(View.INVISIBLE);
            listAdapter.updateList(activity.allUsers);
            lv.setAdapter(listAdapter);
            lv.setVisibility(View.VISIBLE);
        }
    }

    public void showUserForm() {
        userFormDialog = new UserFormDialog(activity);
        userFormDialog.show();
    }

    public void closeUserForm() {
        userFormDialog.dismiss();
    }

    public int getUserPosition(String uid) {
        for (int i = 0; i < activity.allUsers.size(); i++) {
            if (activity.allUsers.get(i).getId().equals(uid)) {
                return i;
            }
        }
        return -1;
    }

    public User getUserById(String uid) {
        for (int i = 0; i < activity.allUsers.size(); i++) {
            if (activity.allUsers.get(i).getId().equals(uid)) {
                return activity.allUsers.get(i);
            }
        }
        return null;
    }

    public User getUserByNfc(String nfcId) {
        for (int i = 0; i < activity.allUsers.size(); i++) {
            if (activity.allUsers.get(i).getNfc().equals(nfcId)) {
                return activity.allUsers.get(i);
            }
        }
        return null;
    }

    public void sortUsersList(List<User> usersList) {
        // Case insensitive sort by surname and name
        Collections.sort(usersList, new Comparator<User>(){
            @Override
            public int compare(User a, User b) {
                int res = a.getSurname().compareToIgnoreCase(b.getSurname());
                if (res == 0) res = a.getName().compareToIgnoreCase(b.getName());
                return res;
            }
        });
    }

    public void editUser(String uid) {
        user = activity.allUsers.get(getUserPosition(uid));
        showUserForm();
        userFormDialog.setData(user);
    }

    public void nfcRead(String nfcId) {
        if (userFormDialog != null) {
            User u = getUserByNfc(nfcId);
            // If Nfc device is already assigned to another user, show alert toast
            if (u != null && u != user) {
                Toast.makeText(activity, getString(R.string.error_user_nfc_assigned), Toast.LENGTH_SHORT).show();
            } else {
                userFormDialog.setNfc(nfcId);
            }
        }
    }

    public void saveUser() {
        // Prevent double click on save button
        userFormDialog.disableButtons();

        String name = userFormDialog.getName();
        String surname = userFormDialog.getSurname();

        userFormDialog.enableButtons();

        if (name.length() < 2 || surname.length() < 2) {
            Toast.makeText(activity, getString(R.string.error_user_shortname), Toast.LENGTH_SHORT).show();
        } else {
            closeUserForm();
            showLoading();

            final User newUser = new User(account.getUid(), userFormDialog.getName(), userFormDialog.getSurname(), userFormDialog.getNfc());

            if (user != null && !user.getId().isEmpty()) {
                // Editing user
                newUser.setId(user.getId());
                user = null;

                db.collection("users").document(newUser.getId())
                    .set(newUser)
                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            // Prevent Fragment not attached to Activity error after logout
                            if (isAdded()) {
                                if (task.isSuccessful()) {
                                    Toast.makeText(activity, getString(R.string.user_modified), Toast.LENGTH_SHORT).show();
                                    activity.allUsers.set(getUserPosition(newUser.getId()), newUser);
                                    updateList();
                                    hideLoading();
                                } else {
                                    Toast.makeText(activity, getString(R.string.error_user_mod), Toast.LENGTH_SHORT).show();
                                    hideLoading();
                                }
                            }
                        }
                    });
            } else {
                // Creating user
                DocumentReference docRef = db.collection("users").document();
                newUser.setId(docRef.getId());

                docRef
                    .set(newUser)
                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            // Prevent Fragment not attached to Activity error after logout
                            if (isAdded()) {
                                if (task.isSuccessful()) {
                                    Toast.makeText(activity, getString(R.string.user_added), Toast.LENGTH_SHORT).show();

                                    activity.allUsers.add(newUser);
                                    updateList();
                                    hideLoading();
                                } else {
                                    Toast.makeText(activity, getString(R.string.error_user_add), Toast.LENGTH_SHORT).show();
                                    hideLoading();
                                }
                            }
                        }
                    });
            }
        }
    }

    public void delUser(final String uid) {
        user = activity.allUsers.get(getUserPosition(uid));

        new AlertDialog.Builder(activity)
            .setTitle(user.toString())
            .setMessage(getString(R.string.del_user) + ". " + getString(R.string.sure))
            .setPositiveButton(getString(R.string.confirm), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    showLoading();

                    db.collection("users").document(uid)
                        .delete()
                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                // Prevent Fragment not attached to Activity error after logout
                                if (isAdded()) {
                                    if (task.isSuccessful()) {
                                        Toast.makeText(activity, getString(R.string.user_deleted), Toast.LENGTH_SHORT).show();

                                        int position = getUserPosition(uid);
                                        activity.allUsers.remove(position);
                                        filterList();
                                        activity.delLunchdateByUser(uid);
                                        hideLoading();
                                    } else {
                                        Toast.makeText(activity, getString(R.string.error_user_del), Toast.LENGTH_SHORT).show();
                                        hideLoading();
                                    }
                                }
                            }
                        });
                    user = null;

                }
            })
            .setNegativeButton(getString(R.string.cancel), null)
            .show();
    }

}
