package com.lopicard.sonial;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class ShoppingCart extends Fragment {
    private View _view;

    public ShoppingCart() {
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        this._view = inflater.inflate(R.layout.fragment_shopping_cart, container, false);
        this._view.findViewById(R.id.layOrder).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Misc.Log("GoToOrder");
                Misc.startActivity(ShoppingCart.this.getContext(), OrderActivity.class);
            }
        });
        this.refresh();
        return this._view;
    }

    public void refresh() {
        ((TextView) this._view.findViewById(R.id.txtCount))
                .setText(String.valueOf(Applica.Current.ChersiManager.CurrentChersi.getTotalItemsCount()));
        ((TextView) this._view.findViewById(R.id.txtPrice))
                .setText(Misc.formatPrice(Applica.Current.ChersiManager.CurrentChersi.getItemsPrice(), true, true));
    }
}