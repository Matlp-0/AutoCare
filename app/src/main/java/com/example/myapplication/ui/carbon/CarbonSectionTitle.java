package com.example.myapplication.ui.carbon;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.example.myapplication.R;

/**
 * Rótulo de seção do HUD: texto curto em caixa alta + fio estrutural que ocupa o
 * resto da linha.
 */
public class CarbonSectionTitle extends LinearLayout {

    private final TextView label;

    public CarbonSectionTitle(Context context) {
        this(context, null);
    }

    public CarbonSectionTitle(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        setOrientation(HORIZONTAL);
        setGravity(Gravity.CENTER_VERTICAL);

        label = new TextView(context, null, 0, R.style.Carbon_Text_Label);
        addView(label, new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));

        View rule = new View(context);
        rule.setBackgroundColor(ContextCompat.getColor(context, R.color.carbon_line_strong));
        LayoutParams ruleParams = new LayoutParams(0,
                getResources().getDimensionPixelSize(R.dimen.carbon_divider), 1f);
        ruleParams.setMarginStart(getResources().getDimensionPixelSize(R.dimen.carbon_space_sm));
        addView(rule, ruleParams);

        if (attrs != null) {
            TypedArray array = context.obtainStyledAttributes(attrs, new int[]{android.R.attr.text});
            CharSequence text = array.getText(0);
            if (text != null) {
                label.setText(text);
            }
            array.recycle();
        }
    }

    public void setText(CharSequence text) {
        label.setText(text);
    }
}
