package com.example.myapplication.ui.carbon;

import android.content.Context;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.example.myapplication.R;

/**
 * Abas de filtro do Carbon UI: rótulos em caixa alta e indicador linear que
 * desliza para a aba selecionada. Mesma linguagem da bottom navigation
 * (amarelo ácido + linha inferior), agora dentro do conteúdo.
 *
 * <p>Só controla apresentação e seleção — quem decide o que cada filtro faz é a tela.
 */
public class CarbonFilterTabs extends LinearLayout {

    public interface OnTabSelected {
        void onTabSelected(int index);
    }

    private final LinearLayout row;
    private final View indicator;

    private OnTabSelected listener;
    private int selectedIndex;

    public CarbonFilterTabs(Context context) {
        this(context, null);
    }

    public CarbonFilterTabs(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        setOrientation(VERTICAL);

        row = new LinearLayout(context);
        row.setOrientation(HORIZONTAL);
        addView(row, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));

        FrameLayout track = new FrameLayout(context);
        track.setBackgroundColor(ContextCompat.getColor(context, R.color.carbon_line));
        addView(track, new LayoutParams(LayoutParams.MATCH_PARENT,
                getResources().getDimensionPixelSize(R.dimen.carbon_rule)));

        indicator = new View(context);
        indicator.setBackgroundColor(ContextCompat.getColor(context, R.color.carbon_accent));
        track.addView(indicator, new FrameLayout.LayoutParams(0,
                FrameLayout.LayoutParams.MATCH_PARENT));
    }

    public void setOnTabSelected(OnTabSelected listener) {
        this.listener = listener;
    }

    public void setTabs(CharSequence... labels) {
        row.removeAllViews();
        for (int index = 0; index < labels.length; index++) {
            final int position = index;
            TextView tab = new TextView(getContext(), null, 0, R.style.Carbon_Text_Value);
            tab.setText(labels[index]);
            tab.setTextSize(12f);
            tab.setGravity(Gravity.CENTER);
            tab.setMinHeight(getResources().getDimensionPixelSize(R.dimen.carbon_touch_min));
            tab.setClickable(true);
            tab.setFocusable(true);
            tab.setBackgroundResource(R.drawable.carbon_detail_press);
            tab.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View view) {
                    select(position, true);
                    if (listener != null) {
                        listener.onTabSelected(position);
                    }
                }
            });
            row.addView(tab, new LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f));
        }
        select(Math.min(selectedIndex, labels.length - 1), false);
    }

    public int getSelectedIndex() {
        return selectedIndex;
    }

    /** Seleciona sem disparar o listener (usado para restaurar estado). */
    public void select(int index, boolean animate) {
        if (index < 0 || index >= row.getChildCount()) {
            return;
        }
        selectedIndex = index;
        for (int position = 0; position < row.getChildCount(); position++) {
            TextView tab = (TextView) row.getChildAt(position);
            boolean active = position == index;
            tab.setTextColor(ContextCompat.getColor(getContext(),
                    active ? R.color.carbon_accent : R.color.carbon_text_secondary));
            tab.setAlpha(active ? 1f : getResources().getFraction(
                    R.fraction.carbon_alpha_muted, 1, 1));
        }
        moveIndicator(animate);
    }

    @Override
    protected void onSizeChanged(int width, int height, int oldWidth, int oldHeight) {
        super.onSizeChanged(width, height, oldWidth, oldHeight);
        moveIndicator(false);
    }

    private void moveIndicator(boolean animate) {
        int count = row.getChildCount();
        if (count == 0 || getWidth() == 0) {
            return;
        }
        int tabWidth = getWidth() / count;
        ViewGroup.LayoutParams params = indicator.getLayoutParams();
        if (params.width != tabWidth) {
            params.width = tabWidth;
            indicator.setLayoutParams(params);
        }
        float target = tabWidth * selectedIndex;
        if (!animate) {
            indicator.setTranslationX(target);
            return;
        }
        indicator.animate()
                .translationX(target)
                .setDuration(getResources().getInteger(R.integer.carbon_duration_state))
                .setInterpolator(new DecelerateInterpolator())
                .start();
    }
}
