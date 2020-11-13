package com.lopicard.sonial;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.TextView;

public class SectionActivity extends AppCompatActivity implements ICommandResult {
    private Section section;
    private RecyclerView mainRecycler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_section);
        // Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        // setSupportActionBar(toolbar);

//        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
//        fab.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
//                        .setAction("Action", null).show();
//            }
//        });
//        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        this.init();
    }

    private void init() {
        this.section = Applica.Current.CurrentSection;
        ((TextView) this.findViewById(R.id.txtSectionName)).setText(this.section.Name);

        this.findViewById(R.id.imgBack).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                SectionActivity.this.onBackPressed();
            }
        });

        this.getSupportFragmentManager().beginTransaction().replace(R.id.fragment_search, new SearchFragment()).commit();
        this.getSupportFragmentManager().beginTransaction().replace(R.id.fragment_shoppingCart, new ShoppingCart()).commit();

        final RecyclerView groupTextRecycler = (RecyclerView) this.findViewById(R.id.textOnlyGroupRecycler);
        groupTextRecycler.setHasFixedSize(true);
        groupTextRecycler.setLayoutManager(new GridLayoutManager(this, 1, LinearLayoutManager.HORIZONTAL, false));
        groupTextRecycler.setAdapter(new GroupAdapter(this, true, this.section.getGroups(), this.findViewById(R.id.layAll)));

        this.mainRecycler = (RecyclerView) this.findViewById(R.id.mainRecycler);
        this.mainRecycler.setHasFixedSize(true);
        this.mainRecycler.setLayoutManager(new GridLayoutManager(this, 1));

        this.findViewById(R.id.layAll).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                SectionActivity.this.switchAdapter(true, null);
                ((GroupAdapter) groupTextRecycler.getAdapter()).setSelectedGroup(null);
            }
        });

        if (this.section.getGroups().size() > 0)
            this.switchAdapter(true, this.section.getGroups().get(0));
    }

    public void switchAdapter(boolean displayAll, Group group) {
        if (displayAll) {
            this.mainRecycler.setAdapter(new GroupAdapter(this, false, this.section.getGroups(), null));
        } else {
            this.mainRecycler.setAdapter(new ItemAdapter(group.getItems(), false));
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Misc.Log("onResume - SectionActivity");
        this.refreshFragments();
    }

    @Override
    public void onBackPressed() {
        Misc.Log("onBackPressed");
        SearchFragment frag = (SearchFragment) this.getSupportFragmentManager().findFragmentById(R.id.fragment_search);
        if (frag.isOpen())
            frag.close();
        else
            super.onBackPressed();
    }

    @Override
    public void reply(Object obj) {
        Group group = (Group) obj;
        this.switchAdapter(false, group);
    }

    private void refreshFragments() {
        ((ShoppingCart) this.getSupportFragmentManager().findFragmentById(R.id.fragment_shoppingCart)).refresh();
    }
}
