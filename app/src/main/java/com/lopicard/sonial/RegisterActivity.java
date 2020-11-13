package com.lopicard.sonial;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class RegisterActivity extends AppCompatActivity implements View.OnClickListener {
    private Address address;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        this.findViewById(R.id.btnSave).setOnClickListener(this);

        if (Applica.Current.TempAddress == null) {
            Applica.Current.TempAddress = new Address();
        }

        this.address = Applica.Current.TempAddress;

        this.getData(this.address);
    }

    private void getData(Address address) {
        ((EditText) this.findViewById(R.id.txtName)).setText(address.Name);
        ((EditText) this.findViewById(R.id.txtEmail)).setText(address.Email);
        ((EditText) this.findViewById(R.id.txtMobile)).setText(address.Mobile);
        ((EditText) this.findViewById(R.id.txtPhone)).setText(address.Phone);
        ((EditText) this.findViewById(R.id.txtAddress)).setText(address.Street);
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.btnSave) {
            this.address.Name = ((TextView) this.findViewById(R.id.txtName)).getText().toString().trim();
            this.address.Email = ((TextView) this.findViewById(R.id.txtEmail)).getText().toString().trim();
            this.address.Phone = ((TextView) this.findViewById(R.id.txtPhone)).getText().toString().trim();
            this.address.Mobile = ((TextView) this.findViewById(R.id.txtMobile)).getText().toString().trim();
            this.address.Street = ((TextView) this.findViewById(R.id.txtAddress)).getText().toString().trim();

            this.address.City = Misc.getStringFromRes(R.string.default_city);

            if (this.address.Name.length() == 0
                    || this.address.City.length() == 0
                    || this.address.Mobile.length() == 0
                    || this.address.Street.length() == 0) {
                Toast.makeText(view.getContext(), Misc.MsgEnterRequiredData, Toast.LENGTH_SHORT).show();
                return;
            }

            this.address.IsRegistered = true;
            Applica.Current.Database.saveAddress(this.address);

            Misc.startActivity(this, MainActivity.class);
            this.finish();
        }
    }
}
