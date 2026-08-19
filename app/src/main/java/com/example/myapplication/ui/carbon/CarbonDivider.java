package com.example.myapplication.ui.carbon;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.example.myapplication.R;

/** Divisor de 1dp do HUD. Substitui as bordas de card do layout antigo. */
public class CarbonDivider extends View {

    public CarbonDivider(Context context) {
        this(context, null);
    }

    public CarbonDivider(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        setBackgroundColor(ContextCompat.getColor(context, R.color.carbon_line));
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int height = getResources().getDimensionPixelSize(R.dimen.carbon_divider);
        super.onMeasure(widthMeasureSpec, MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY));
    }
}
