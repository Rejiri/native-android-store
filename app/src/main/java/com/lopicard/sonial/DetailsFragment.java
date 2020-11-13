package com.lopicard.sonial;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;

public class DetailsFragment extends Fragment {
    public DetailsFragment() {
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_details, container, false);

        view.findViewById(R.id.imgBack).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                DetailsFragment.this.getActivity().onBackPressed();
            }
        });

        view.findViewById(R.id.txtTitle).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View view) {
                new MaterialDialog.Builder(view.getContext())
                        .items(new String[]{Misc.getStringFromRes(R.string.label_mr), Misc.getStringFromRes(R.string.label_mrs)})
                        .itemsCallback(new MaterialDialog.ListCallback() {
                            @Override
                            public void onSelection(MaterialDialog dialog, View itemView, int position, CharSequence text) {
                                ((TextView) view).setText(text);
                            }
                        }).show();
            }
        });
        view.findViewById(R.id.btnSave).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Address address = new Address();
                address.Name = ((TextView) DetailsFragment.this.getView().findViewById(R.id.txtName)).getText().toString().trim();
                address.City = ((TextView) DetailsFragment.this.getView().findViewById(R.id.txtCity)).getText().toString().trim();
                address.Phone = ((TextView) DetailsFragment.this.getView().findViewById(R.id.txtPhone)).getText().toString().trim();
                address.Mobile = ((TextView) DetailsFragment.this.getView().findViewById(R.id.txtMobile)).getText().toString().trim();
                address.Street = ((TextView) DetailsFragment.this.getView().findViewById(R.id.txtAddress)).getText().toString().trim();

                if (address.Name.length() == 0
                        || address.City.length() == 0
                        || address.Mobile.length() == 0
                        || address.Street.length() == 0) {
                    Toast.makeText(view.getContext(), "Please enter all required data", Toast.LENGTH_SHORT).show();
                    return;
                }
                Applica.Current.Database.saveAddress(address);
                Misc.startActivity(DetailsFragment.this.getContext(), AddressActivity.class);
            }
        });
        return view;
    }
}