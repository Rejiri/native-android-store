package com.lopicard.sonial;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.graphics.drawable.RoundedBitmapDrawable;
import android.support.v4.graphics.drawable.RoundedBitmapDrawableFactory;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.BitmapImageViewTarget;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener, View.OnClickListener, ICommandResult {

    private ArrayList<Section> sections;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

//        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
//        fab.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
//                        .setAction("Action", null).show();
//            }
//        });

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        this.init();
        // this.switchProgress(true);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Misc.Log("MainActivity - onResume");
        this.refreshFragments();
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
//            if (this.getSupportFragmentManager().getBackStackEntryCount() > 0) {
//                this.getSupportFragmentManager().popBackStack();
//                Misc.Log("popBackStack");
//            } else
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_camera) {
            // Handle the camera action
        } else if (id == R.id.nav_gallery) {

        } else if (id == R.id.nav_slideshow) {

        } else if (id == R.id.nav_manage) {

        } else if (id == R.id.nav_share) {

        } else if (id == R.id.nav_send) {

        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    private void init() {
        ((TextView) ((NavigationView) this.findViewById(R.id.nav_view)).getHeaderView(0).findViewById(R.id.txtUserName)).setText(Applica.Current.getUser().Name);
        ((TextView) ((NavigationView) this.findViewById(R.id.nav_view)).getHeaderView(0).findViewById(R.id.txtUserPhone)).setText(Applica.Current.getUser().Mobile);
        ((NavigationView) this.findViewById(R.id.nav_view)).getHeaderView(0).findViewById(R.id.navShoppingCart).setOnClickListener(this);
        ((NavigationView) this.findViewById(R.id.nav_view)).getHeaderView(0).findViewById(R.id.navHistory).setOnClickListener(this);
        ((NavigationView) this.findViewById(R.id.nav_view)).getHeaderView(0).findViewById(R.id.navRate).setOnClickListener(this);
        ((NavigationView) this.findViewById(R.id.nav_view)).getHeaderView(0).findViewById(R.id.navShare).setOnClickListener(this);
        ((NavigationView) this.findViewById(R.id.nav_view)).getHeaderView(0).findViewById(R.id.navDevelopers).setOnClickListener(this);
        ((NavigationView) this.findViewById(R.id.nav_view)).getHeaderView(0).findViewById(R.id.navAboutUs).setOnClickListener(this);

        if (!(Applica.Current.getUser().Photo == null)) {
            final ImageView imgUserPhoto = (ImageView) ((NavigationView) this.findViewById(R.id.nav_view)).getHeaderView(0).findViewById(R.id.imgUserPhoto);

            Glide.with(this).load(Applica.Current.getUser().Photo).asBitmap().centerCrop().into(new BitmapImageViewTarget(imgUserPhoto) {
                @Override
                protected void setResource(Bitmap resource) {
                    RoundedBitmapDrawable circularBitmapDrawable =
                            RoundedBitmapDrawableFactory.create(MainActivity.this.getResources(), resource);
                    circularBitmapDrawable.setCircular(true);
                    imgUserPhoto.setImageDrawable(circularBitmapDrawable);
                }
            });
        }

        this.getSupportFragmentManager().beginTransaction().replace(R.id.fragment_shoppingCart, new ShoppingCart()).commit();

        GridLayoutManager gridLayoutManager = new GridLayoutManager(this, 2);
        gridLayoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                return (position == 0) ? 2 : 1;
            }
        });
        RecyclerView recyclerView = (RecyclerView) this.findViewById(R.id.mainRecycler);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(gridLayoutManager);
        recyclerView.setAdapter(new SectionAdapter(Applica.Current.Database.getSections()));
    }

    private void refreshFragments() {
        ((ShoppingCart) this.getSupportFragmentManager().findFragmentById(R.id.fragment_shoppingCart)).refresh();
        ((TextView) ((NavigationView) this.findViewById(R.id.nav_view)).getHeaderView(0).findViewById(R.id.txtNavCartCount))
                .setText(String.valueOf(Applica.Current.ChersiManager.CurrentChersi.getTotalItemsCount()));
    }

    private void switchProgress(boolean value) {
        if (value) {
            this.findViewById(R.id.progressBar).setVisibility(View.VISIBLE);
            this.findViewById(R.id.mainRecycler).setVisibility(View.GONE);
        } else {
            this.findViewById(R.id.progressBar).setVisibility(View.GONE);
            this.findViewById(R.id.mainRecycler).setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.navShoppingCart)
            Misc.startActivity(this, OrderActivity.class);
        else if (view.getId() == R.id.navHistory)
            Misc.startActivity(this, HistoryActivity.class);
        else if (view.getId() == R.id.navShare) {
            Intent sharingIntent = new Intent(android.content.Intent.ACTION_SEND);
            sharingIntent.setType("text/plain");
            String shareBody = "https://play.google.com/store/apps/details?id=com.lopicard.sonial";
            sharingIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, "Talabatcom");
            sharingIntent.putExtra(android.content.Intent.EXTRA_TEXT, shareBody);
            this.startActivity(Intent.createChooser(sharingIntent, "Share via"));
        } else if (view.getId() == R.id.navRate) {
            this.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=com.lopicard.sonial")));
        } else if (view.getId() == R.id.navDevelopers)
            new MaterialDialog.Builder(this).customView(R.layout.dialog_developer, true).show();
        else if (view.getId() == R.id.navAboutUs)
            new MaterialDialog.Builder(this).customView(R.layout.dialog_about_us, true).show();
    }

    @Override
    public void reply(Object obj) {
        this.switchProgress(!((boolean) obj));
        if (!(boolean) obj)
            Toast.makeText(this, Misc.MsgCheckNetwork, Toast.LENGTH_SHORT).show();

        this.init();
    }
}
