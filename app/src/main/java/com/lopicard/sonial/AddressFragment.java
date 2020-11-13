package com.lopicard.sonial;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import org.json.JSONException;

import java.util.ArrayList;

public class AddressFragment extends Fragment implements View.OnClickListener {
    private View view;
    private View progressBar;

    public AddressFragment() {
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        this.view = inflater.inflate(R.layout.fragment_address, container, false);

        this.progressBar = this.view.findViewById(R.id.progressBar);

        RecyclerView addressRecycler = ((RecyclerView) this.view.findViewById(R.id.addressRecycler));
        addressRecycler.setHasFixedSize(true);
        addressRecycler.setLayoutManager(new GridLayoutManager(this.getContext(), 1));
        addressRecycler.setAdapter(new AddressAdapter(Applica.Current.Database.getAddresses(), true));

        this.view.findViewById(R.id.imgBack).setOnClickListener(this);
        this.view.findViewById(R.id.btnAdd).setOnClickListener(this);
        this.view.findViewById(R.id.layEnd).setOnClickListener(this);

        return this.view;
    }

    private void send() {
        try {
            Address address = null;
            ArrayList<Address> aCol = Applica.Current.Database.getAddresses();
            for (int i = 0; i < aCol.size(); i++)
                if (aCol.get(i).IsDefault)
                    address = aCol.get(i);
            if (address == null) {
                Toast.makeText(AddressFragment.this.getContext(), Misc.MsgSelectDeliveryAddress, Toast.LENGTH_SHORT).show();
                return;
            }

            this.progressBar.setVisibility(View.VISIBLE);
            Applica.Current.ChersiManager.CurrentChersi.deliveryNote = ((EditText) this.view.findViewById(R.id.txtNote)).getText().toString();
            Applica.Current.ChersiManager.CurrentChersi.address = address;
            Applica.Current.ChersiManager.CurrentChersi.send(view.getContext(), new ICommandResult() {
                @Override
                public void reply(Object obj) {
                    if (obj == null) {
                        AddressFragment.this.progressBar.setVisibility(View.GONE);
                        Toast.makeText(AddressFragment.this.getContext(), Misc.MsgPleaseTryAgain, Toast.LENGTH_SHORT).show();
                    } else
                        Misc.startActivity(AddressFragment.this.getContext(), HistoryActivity.class);
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
            this.progressBar.setVisibility(View.GONE);
            Toast.makeText(this.getContext(), Misc.MsgPleaseTryAgain, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.imgBack)
            this.getActivity().onBackPressed();
        else if (v.getId() == R.id.btnAdd)
            Misc.startActivity(this.getContext(), InfoActivity.class);
        else if (v.getId() == R.id.layEnd)
            this.send();
    }
}