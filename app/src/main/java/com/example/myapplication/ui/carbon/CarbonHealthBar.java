package com.example.myapplication.ui.carbon;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.example.myapplication.R;

/**
 * Barra HUD segmentada (0-100). Puramente visual: recebe a nota já calculada pelo
 * domínio e não faz nenhuma conta de saúde.
 */
public class CarbonHealthBar extends View {

    private static final int SEGMENTS = 24;

    private final Paint segmentPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint trackPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private float progress;
    private int filledColor;
    private int trackColor;
    private ValueAnimator animator;

    public CarbonHealthBar(Context context) {
        this(context, null);
    }

    public CarbonHealthBar(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        filledColor = ContextCompat.getColor(context, R.color.carbon_cyan);
        trackColor = ContextCompat.getColor(context, R.color.carbon_line);
        segmentPaint.setStyle(Paint.Style.FILL);
        trackPaint.setStyle(Paint.Style.FILL);
        trackPaint.setColor(trackColor);
    }

    /** @param score 0-100, já validado pelo domínio. */
    public void setScore(int score, int colorRes, boolean animate) {
        filledColor = ContextCompat.getColor(getContext(), colorRes);
        float target = Math.max(0f, Math.min(100f, score)) / 100f;
        if (animator != null) {
            animator.cancel();
        }
        if (!animate) {
            progress = target;
            invalidate();
            return;
        }
        animator = ValueAnimator.ofFloat(progress, target);
        animator.setDuration(getResources().getInteger(R.integer.carbon_duration_state));
        animator.setInterpolator(new DecelerateInterpolator());
        animator.addUpdateListener(animation -> {
            progress = (float) animation.getAnimatedValue();
            invalidate();
        });
        animator.start();
    }

    @Override
    protected void onDetachedFromWindow() {
        if (animator != null) {
            animator.cancel();
        }
        super.onDetachedFromWindow();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int width = getWidth();
        int height = getHeight();
        if (width == 0 || height == 0) {
            return;
        }

        float gap = height * 0.28f;
        float segmentWidth = (width - gap * (SEGMENTS - 1)) / SEGMENTS;
        int filled = Math.round(progress * SEGMENTS);

        for (int index = 0; index < SEGMENTS; index++) {
            float left = index * (segmentWidth + gap);
            boolean on = index < filled;
            segmentPaint.setColor(on ? filledColor : trackColor);
            // Segmentos inativos ficam mais baixos: leitura de painel, não de progresso.
            float top = on ? 0f : height * 0.35f;
            canvas.drawRect(left, top, left + segmentWidth, height, on ? segmentPaint : trackPaint);
        }
    }
}
