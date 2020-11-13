package com.lopicard.sonial;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import org.json.JSONObject;

public class PharmacyFragment extends Fragment implements ICommandResult {
    public PharmacyFragment() {
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_pharmacy, container, false);
        view.findViewById(R.id.btnSubmit).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view2) {
                new Prescription(((EditText) view.findViewById(R.id.txtPrescription)).getText().toString()).send(PharmacyFragment.this);
            }
        });
        return view;
    }

    @Override
    public void reply(Object obj) {
        if (obj instanceof JSONObject) {
            Toast.makeText(this.getContext(), Misc.MsgPrescriptionHasBeenSent, Toast.LENGTH_SHORT).show();
            this.getActivity().finish();
        } else
            Toast.makeText(this.getContext(), Misc.MsgError, Toast.LENGTH_SHORT).show();
    }
}
