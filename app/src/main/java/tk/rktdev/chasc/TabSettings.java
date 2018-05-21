package tk.rktdev.chasc;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.firebase.ui.auth.AuthUI;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

public class TabSettings extends Fragment {
    private TabsActivity activity;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.tab_settings, container, false);
        activity = (TabsActivity) getActivity();

        return rootView;
    }

    public void logout() {
        AuthUI.getInstance()
            .signOut(activity)
            .addOnCompleteListener(new OnCompleteListener<Void>() {
                public void onComplete(@NonNull Task<Void> task) {
                    startActivity(new Intent(activity, SplashActivity.class));
                    activity.finish();
                }
            });
    }
}
