package com.lopicard.sonial;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

public class PharmacyActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pharmacy);

        this.getSupportFragmentManager().beginTransaction().replace(R.id.fragment_one, new PharmacyFragment()).commit();
    }
}
