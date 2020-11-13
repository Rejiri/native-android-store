package com.lopicard.sonial;

import android.content.Context;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.widget.TextView;

public class TextView2 extends TextView {
    public static String namespace = "http://schemas.android.com/apk/res-auto";

    public TextView2(Context context) {
        super(context);
    }

    public TextView2(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public TextView2(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        if (this.isInEditMode()) return;

        this.setFont(this, attrs.getAttributeIntValue(TextView2.namespace, "fonti", 0));

//        int localTextIndex = attrs.getAttributeIntValue(Misc.namespace, "localText", -1);
//        if (!(localTextIndex == -1))
//            this.setText(Loca.getLocalText(localTextIndex));
    }

    private void setFont(TextView textView, int typefaceIdentifier) {
        String fontName = null;

        if (typefaceIdentifier == 1)
            fontName = "BigVesta_Arabic_Regular.ttf";
        else if (typefaceIdentifier == 2)
            fontName = "BigVesta_Arabic_Bold.ttf";
        else if (typefaceIdentifier == 3)
            fontName = "BAUHS93.TTF";
        else if (typefaceIdentifier == 4)
            fontName = "HacenSaudiArabia.ttf";

        if (fontName == null) return;
        textView.setTypeface(Typeface.createFromAsset(Applica.Current.getAssets(), Misc.format("typefaces/%s", fontName)));
    }
}