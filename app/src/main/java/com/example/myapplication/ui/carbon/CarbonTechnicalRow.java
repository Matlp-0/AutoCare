package com.example.myapplication.ui.carbon;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.example.myapplication.R;

/**
 * Linha técnica do HUD: <em>RÓTULO .............. VALOR</em>.
 * Base das seções de ficha técnica e dos resumos das listas.
 */
public class CarbonTechnicalRow extends LinearLayout {

    private final TextView label;
    private final TextView value;

    public CarbonTechnicalRow(Context context) {
        this(context, null);
    }

    public CarbonTechnicalRow(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        setOrientation(HORIZONTAL);
        setGravity(Gravity.CENTER_VERTICAL);
        setBaselineAligned(false);
        int vertical = getResources().getDimensionPixelSize(R.dimen.carbon_space_sm);
        setPadding(0, vertical, 0, vertical);

        label = new TextView(context, null, 0, R.style.Carbon_Text_Caption);
        label.setAllCaps(true);
        addView(label, new LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f));

        value = new TextView(context, null, 0, R.style.Carbon_Text_Value);
        value.setGravity(Gravity.END);
        addView(value, new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));

        if (attrs != null) {
            TypedArray array = context.obtainStyledAttributes(attrs,
                    new int[]{android.R.attr.text, android.R.attr.hint});
            CharSequence labelText = array.getText(0);
            CharSequence valueText = array.getText(1);
            if (labelText != null) {
                label.setText(labelText);
            }
            if (valueText != null) {
                value.setText(valueText);
            }
            array.recycle();
        }
    }

    public void setLabel(CharSequence text) {
        label.setText(text);
    }

    public void setValue(CharSequence text) {
        value.setText(text);
    }

    public void setValueColor(int colorRes) {
        value.setTextColor(ContextCompat.getColor(getContext(), colorRes));
    }
}
