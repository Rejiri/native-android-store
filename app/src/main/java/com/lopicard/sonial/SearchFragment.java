package com.lopicard.sonial;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import java.util.ArrayList;

public class SearchFragment extends Fragment {
    private RecyclerView searchRecycler;

    public SearchFragment() {
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_search, container, false);

//        view.setFocusableInTouchMode(true);
//        view.requestFocus();
//        view.setOnKeyListener(new View.OnKeyListener() {
//            @Override
//            public boolean onKey(View view, int i, KeyEvent keyEvent) {
//                if (i == KeyEvent.KEYCODE_BACK)
//                    Misc.Log("Back");
//                return true;
//            }
//        });

        ((TextView) view.findViewById(R.id.txtCount)).setText(Misc.format("%d صنف متوفر", Applica.Current.CurrentSection.itemsCount()));
        this.searchRecycler = (RecyclerView) view.findViewById(R.id.searchRecycler);
        this.searchRecycler.setHasFixedSize(true);
        this.searchRecycler.setLayoutManager(new GridLayoutManager(this.getContext(), 1, LinearLayoutManager.VERTICAL, false));

        ((EditText) view.findViewById(R.id.txtSearch)).addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void afterTextChanged(Editable editable) {
                SearchFragment.this.doSearch(editable.toString());
            }
        });

//        ((EditText) view.findViewById(R.id.txtSearch)).setOnFocusChangeListener(new View.OnFocusChangeListener() {
//            @Override
//            public void onFocusChange(View view, boolean b) {
//                Misc.Log("%b", b);
//            }
//        });
        return view;
    }

    private void doSearch(String text) {
        Misc.Log(text);

        if (text.length() == 0)
            this.searchRecycler.setVisibility(View.GONE);
        else {
            ArrayList<Item> items = Applica.Current.Database.searchFor(text);
            if (items.size() > 0) {
                this.searchRecycler.setAdapter(new SearchAdapter(this, items));
                this.searchRecycler.setVisibility(View.VISIBLE);
            } else
                this.searchRecycler.setVisibility(View.GONE);
        }
    }

    public boolean isOpen() {
        return this.searchRecycler.getVisibility() == View.VISIBLE;
    }

    public void close() {
        this.searchRecycler.setVisibility(View.GONE);
    }
}