package com.lopicard.sonial;

import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

public class LogoActivity extends AppCompatActivity implements ICommandResult {
    private Handler handler;
    private Runnable runnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_logo);

        this.handler = new Handler();
        this.runnable = new Runnable() {
            @Override
            public void run() {
                LogoActivity.this.checkAndRun();
            }
        };

        try {
            Applica.Current.fetchData(this);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void checkAndRun() {
        if (Applica.Current.getUser().IsRegistered)
            Misc.startActivity(this, MainActivity.class);
        else
            Misc.startActivity(this, SignInActivity.class);

        this.handler.removeCallbacks(this.runnable);
        this.finish();
    }

    @Override
    public void reply(Object obj) {
        if (obj instanceof String) {
            Toast.makeText(this, obj.toString(), Toast.LENGTH_SHORT).show();
            this.findViewById(R.id.progressBar).setVisibility(View.GONE);
        } else if (obj instanceof Boolean) {
            if ((boolean) obj)
                this.handler.postDelayed(this.runnable, 800);
            else
                Toast.makeText(this, Misc.MsgCheckNetwork, Toast.LENGTH_SHORT).show();
        }
    }
}
