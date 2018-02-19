package com.example.virgilio.chasc;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        DBUtil.setDirectory(getDir("data", Context.MODE_PRIVATE));
    }

    public void admin (View v){
        Intent i = new Intent(this, UsersMain.class);
        startActivity(i);
    }

    public void lunch (View v){
        Intent i = new Intent(this, PublicMain.class);
        startActivity(i);
    }
}
