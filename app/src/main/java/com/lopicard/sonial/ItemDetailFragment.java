package com.lopicard.sonial;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.TextView;

import com.bumptech.glide.Glide;

public class ItemDetailFragment extends Fragment implements View.OnClickListener {
    private ChersiItem chersiItem;
    private View view;

    public ItemDetailFragment() {
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_item_details, container, false);
        this.view = view;
        this.init();
        return view;
    }

    private void init() {
        this.chersiItem = Applica.Current.ChersiManager.CurrentChersi.getItemIfExist(Applica.Current.CurrentItem);
        if (this.chersiItem == null)
            this.chersiItem = new ChersiItem(Applica.Current.CurrentItem, 0);

        this.view.findViewById(R.id.imgBack).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ItemDetailFragment.this.getActivity().onBackPressed();
            }
        });

        ((TextView) this.view.findViewById(R.id.txtName)).setText(this.chersiItem.item.Name);
        ((TextView) this.view.findViewById(R.id.txtDescription)).setText(this.chersiItem.item.Description);
        ((TextView) this.view.findViewById(R.id.txtGroup)).setText(this.chersiItem.item.GroupIn.Name);
        ((TextView) this.view.findViewById(R.id.lblWeight)).setText(this.chersiItem.item.getMassText());
        ((TextView) this.view.findViewById(R.id.txtCapacity)).setText(this.chersiItem.item.getMassFormatted());
        ((TextView) this.view.findViewById(R.id.txtPrice)).setText(Misc.formatPrice(this.chersiItem.item.Price, true, true));
        ((TextView) this.view.findViewById(R.id.txtLikes)).setText(String.valueOf(this.chersiItem.item.LikeCount));
        ((TextView) this.view.findViewById(R.id.txtRequestsCount)).setText(Misc.format("+%d", this.chersiItem.item.RequestsCount));

        if (this.chersiItem.item.GroupIn.withinAvailableTime()) {
            ((TextView) this.view.findViewById(R.id.txtAvailable)).setText(R.string.TVAvailable);
            this.view.findViewById(R.id.layAvailable).setBackgroundResource(R.drawable.roundgreen);
        } else {
            ((TextView) this.view.findViewById(R.id.txtAvailable)).setText(R.string.TVUnAvailable);
            this.view.findViewById(R.id.layAvailable).setBackgroundResource(R.drawable.round);
        }

        if (!(Misc.isNullOrEmpty(this.chersiItem.item.Description)))
            this.view.findViewById(R.id.txtDescription).setVisibility(View.VISIBLE);

        if (Applica.Current.CurrentItem.GroupIn.SectionIn.Id == 0) {
            this.view.findViewById(R.id.layDrinks).setVisibility(View.VISIBLE);

            this.view.findViewById(R.id.rdoPepsi).setOnClickListener(this);
            this.view.findViewById(R.id.rdoPepsiDiet).setOnClickListener(this);
            this.view.findViewById(R.id.rdoSeven).setOnClickListener(this);
            this.view.findViewById(R.id.rdoMirinda).setOnClickListener(this);
            this.view.findViewById(R.id.rdoCola).setOnClickListener(this);
            this.view.findViewById(R.id.rdoSprite).setOnClickListener(this);
            this.view.findViewById(R.id.rdoFanta).setOnClickListener(this);

            this.view.findViewById(R.id.lblWeight).setVisibility(View.INVISIBLE);
            this.view.findViewById(R.id.txtCapacity).setVisibility(View.INVISIBLE);
        }

        Misc.setTextWithColor(
                this.view.findViewById(R.id.txtStatus),
                ((this.chersiItem.item.Available) ? Loca.getValue(Loca.VAvailable) : Loca.getValue(Loca.VUnAvailable)),
                ((this.chersiItem.item.Available) ? Misc.ColorGreen : Misc.ColorRed));

        this.updateQuantity(
                this.chersiItem,
                (TextView) this.view.findViewById(R.id.txtQuantity),
                ((ShoppingCart) this.getActivity().getSupportFragmentManager().findFragmentById(R.id.fragment_shoppingCart)),
                true, false);

        Glide.with(this.getContext()).load(this.chersiItem.item.getDefaultPhotoPath()).into(((ImageView) this.view.findViewById(R.id.imgItem)));

        RecyclerView mainRecycler = (RecyclerView) this.view.findViewById(R.id.mainRecycler);
        mainRecycler.setHasFixedSize(true);
        mainRecycler.setLayoutManager(new GridLayoutManager(this.getContext(), 1, LinearLayoutManager.HORIZONTAL, false));
        mainRecycler.setAdapter(new ItemAdapter(this.chersiItem.item.GroupIn.getItems(), true));

        this.view.findViewById(R.id.txtAdd).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ItemDetailFragment.this.updateQuantity(
                        ItemDetailFragment.this.chersiItem,
                        (TextView) ItemDetailFragment.this.view.findViewById(R.id.txtQuantity),
                        ((ShoppingCart) ItemDetailFragment.this.getActivity().getSupportFragmentManager().findFragmentById(R.id.fragment_shoppingCart)),
                        false, true);
            }
        });
        this.view.findViewById(R.id.txtRemove).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ItemDetailFragment.this.updateQuantity(
                        ItemDetailFragment.this.chersiItem,
                        (TextView) ItemDetailFragment.this.view.findViewById(R.id.txtQuantity),
                        ((ShoppingCart) ItemDetailFragment.this.getActivity().getSupportFragmentManager().findFragmentById(R.id.fragment_shoppingCart)),
                        false, false);
            }
        });
    }

    public static void updateQuantity(ChersiItem chersiItem, TextView quantity, ShoppingCart shoppingCart, boolean justUIValue, boolean addition) {
        if (justUIValue)
            quantity.setText(String.valueOf(chersiItem.quantity));
        else {
            Applica.Current.ChersiManager.addIf(chersiItem, addition);
            quantity.setText(String.valueOf(chersiItem.quantity));
            if (!(shoppingCart == null))
                shoppingCart.refresh();
        }
    }

    public void refresh(boolean justQuantity) {
        if (justQuantity)
            ((TextView) this.view.findViewById(R.id.txtQuantity)).setText(String.valueOf(this.chersiItem.quantity));
    }

    @Override
    public void onClick(View v) {
        Misc.Log("%d", v.getId());

        if (v instanceof RadioButton)
            this.chersiItem.notes = ((RadioButton) v).getText().toString();
    }
}