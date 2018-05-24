package tk.rktdev.chasc;


import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Typeface;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.util.SparseArray;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

// ListAdapter to customize the public users list
public class TabLunchTimeListAdapter extends BaseAdapter {
    private FirebaseFirestore db;

    private List<User> users;
    private List<Lunchdate> lunchdates;
    private TabsActivity activity;
    private Button btnOptions;
    private Map<String, Integer> colorByStatus;

    // Map where link each item position to its spinner to be able to open it dynamically
    private SparseArray<Spinner> spinners;

    public TabLunchTimeListAdapter(List<User> u, List<Lunchdate> l, TabsActivity a) {
        users = u;
        lunchdates = l;
        activity = a;
        db = activity.getDb();

        // Link Lunchdate status to a color to highlight the list rows, including null reference for lunchdates without status
        colorByStatus = new HashMap<>();
        colorByStatus.put(Lunchdate.Event.ATTENDED.toString(), ContextCompat.getColor(activity, R.color.colorSuccess));
        colorByStatus.put(Lunchdate.Event.NO_SHOW.toString(), ContextCompat.getColor(activity, R.color.colorDanger));
        colorByStatus.put(null, 0);
    }

    @Override
    public int getCount() {
        return lunchdates.size();
    }

    @Override
    public Object getItem(int pos) {
        return lunchdates.get(pos);
    }

    @Override
    public long getItemId(int pos) {
        return pos;
    }

    @Override
    public View getView(final int position, View view, ViewGroup parent) {
        if (view == null) {
            LayoutInflater inflater = (LayoutInflater) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            view = inflater.inflate(R.layout.listadapter_tab_lunchtime, null);
        }
        final View currentView = view;

        final User user = users.get(position);
        final Lunchdate lunchdate = lunchdates.get(position);
        currentView.setBackgroundColor(colorByStatus.get(lunchdate.getStatus()));

        TextView listItem = currentView.findViewById(R.id.listItemString);
        if (listItem != null) listItem.setText(user.toString());

        // Show dialog by clicking on the button or anywhere in the current row of the list
        btnOptions = (Button) view.findViewById(R.id.btnOptions);
        btnOptions.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDialog(user, lunchdate, position, currentView);
            }
        });
        currentView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDialog(user, lunchdate, position, currentView);
            }
        });

        return currentView;
    }

    private void showDialog(User user, final Lunchdate lunchdate, final int position, final View currentView) {
        TextView title = new TextView(activity);
        title.setText(user.toString());
        title.setGravity(Gravity.CENTER);
        title.setPadding(0, 50, 0, 50);
        title.setTextSize(18);
        title.setTypeface(null, Typeface.BOLD);
        title.setTextColor(ContextCompat.getColor(activity, R.color.colorAccent));

        final AlertDialog dialog = new AlertDialog.Builder(activity)
            .setView(R.layout.dialog_lunchtime_options)
            .setCustomTitle(title)
            .setNegativeButton(R.string.close, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                }
            })
            .create();

        dialog.show();

        View.OnClickListener listener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (v.getId() == R.id.btnAttended) {
                    tick(lunchdate, Lunchdate.Event.ATTENDED, position, currentView);
                } else if (v.getId() == R.id.btnNoShow) {
                    tick(lunchdate, Lunchdate.Event.NO_SHOW, position, currentView);
                } else if (v.getId() == R.id.btnClearStatus) {
                    tick(lunchdate, null, position, currentView);
                }
                dialog.dismiss();
            }
        };

        dialog.findViewById(R.id.btnAttended).setOnClickListener(listener);
        dialog.findViewById(R.id.btnNoShow).setOnClickListener(listener);
        if (lunchdate.getStatus() == null) {
            dialog.findViewById(R.id.btnClearStatus).setEnabled(false);
        } else {
            dialog.findViewById(R.id.btnClearStatus).setOnClickListener(listener);
        }
    }

    private void tick(final Lunchdate lunchdate, Lunchdate.Event evt, final int pos, final View view) {
        // Mark a Lunchdate with the corresponding Event
        activity.showLunchTimeLoading();

        final String event = (evt == null)? null : evt.toString();
        lunchdate.setStatus(event);

        db.collection("lunchdates")
            .document(lunchdate.getId())
            .set(lunchdate)
            .addOnSuccessListener(new OnSuccessListener<Void>() {
                @Override
                public void onSuccess(Void aVoid) {
                    lunchdates.set(pos, lunchdate);
                    view.setBackgroundColor(colorByStatus.get(event));
                    activity.updateLunchdateStatus(lunchdate);
                    activity.hideLunchTimeLoading();
                }
            })
            .addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Toast.makeText(activity, activity.getString(R.string.error_lunchdate_mod), Toast.LENGTH_SHORT).show();
                    activity.hideLunchTimeLoading();
                }
            });
    }

    public void updateList(List<User> u, List<Lunchdate> l) {
        users = u;
        lunchdates = l;
    }
}
