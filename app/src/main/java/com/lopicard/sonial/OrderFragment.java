package com.lopicard.sonial;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;


public class OrderFragment extends Fragment implements ICommandResult {
    private View view;
    private Chersi chersi;
    private RecyclerView mainRecycler;

    public OrderFragment() {
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        this.chersi = Applica.Current.ChersiManager.CurrentChersi;

        this.view = inflater.inflate(R.layout.fragment_order, container, false);

        this.view.findViewById(R.id.imgBack).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                OrderFragment.this.getActivity().onBackPressed();
            }
        });

        this.setData();

        this.mainRecycler = (RecyclerView) this.view.findViewById(R.id.mainRecycler);
        this.mainRecycler.setHasFixedSize(true);
        this.mainRecycler.setLayoutManager(new GridLayoutManager(this.view.getContext(), 1));
        this.mainRecycler.setAdapter(new OrderAdapter(this.chersi, this));

        this.view.findViewById(R.id.laySave).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                OrderFragment.this.send();
            }
        });

        return this.view;
    }

    private void send() {
        if (this.chersi.items.size() > 0)
            Misc.startActivity(this.getContext(), AddressActivity.class);
        else
            Toast.makeText(this.getContext(), Misc.MsgSelectOneAtLeast, Toast.LENGTH_SHORT).show();
    }

    private void setData() {
        ((TextView) this.view.findViewById(R.id.txtDate)).setText(Misc.getFormattedDateTime(this.chersi.dateTime, "dd/MM/yyyy"));
        ((TextView) this.view.findViewById(R.id.txtTime)).setText(Misc.getFormattedDateTime(this.chersi.dateTime, "h:mm a"));
        ((TextView) this.view.findViewById(R.id.txtItemsPrice)).setText(Misc.formatPrice(this.chersi.getItemsPrice(), true, true));
        ((TextView) this.view.findViewById(R.id.txtInfoBar)).setText(Misc.format("Total items (%d)", this.chersi.items.size()));
        ((TextView) this.view.findViewById(R.id.txtCount)).setText(String.valueOf(this.chersi.getTotalItemsCount()));
        ((TextView) this.view.findViewById(R.id.txtDiscount)).setText(Misc.formatPrice(this.chersi.discount, true, true));
        ((TextView) this.view.findViewById(R.id.txtDeliveryFees)).setText(Misc.formatPrice(this.chersi.deliveryFees, true, true));
        ((TextView) this.view.findViewById(R.id.txtTotalPrice)).setText(Misc.formatPrice(this.chersi.getTotalPrice(), true, true));
    }

    @Override
    public void reply(Object obj) {
        this.setData();
    }
}