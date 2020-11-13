package com.lopicard.sonial;

import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;

public class ItemActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_item);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        // setSupportActionBar(toolbar);

//        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
//        fab.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
//                        .setAction("Action", null).show();
//            }
//        });
        // getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        this.init();
    }

    @Override
    protected void onResume() {
        super.onResume();
        this.refresh();
    }

    private void refresh() {
        ((ItemDetailFragment) this.getSupportFragmentManager().findFragmentById(R.id.fragment_one)).refresh(true);
        ((ShoppingCart) this.getSupportFragmentManager().findFragmentById(R.id.fragment_shoppingCart)).refresh();
    }

    private void init() {
        this.getSupportFragmentManager().beginTransaction().add(R.id.fragment_one, new ItemDetailFragment()).commit();
        this.getSupportFragmentManager().beginTransaction().replace(R.id.fragment_shoppingCart, new ShoppingCart()).commit();
    }
}
