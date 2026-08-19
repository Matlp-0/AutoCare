package com.example.myapplication.ui.hud;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

import androidx.annotation.ColorInt;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.example.myapplication.R;

/**
 * Medidor de saúde segmentado. Barra contínua lê como progresso de download;
 * segmento inclinado lê como painel.
 *
 * <p>Puramente visual: recebe a nota já calculada pelo domínio.
 */
public class SegmentBar extends View {

    private static final int DEFAULT_SEGMENTS = 24;

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path segment = new Path();

    private int segmentCount = DEFAULT_SEGMENTS;
    private float gap;
    private float progress;
    private int accent;
    private int offColor;
    private ValueAnimator animator;

    public SegmentBar(Context context) {
        this(context, null);
    }

    public SegmentBar(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);

        gap = getResources().getDimension(R.dimen.hud_segment_gap);
        accent = ContextCompat.getColor(context, R.color.hud_accent_cyan);
        offColor = ContextCompat.getColor(context, R.color.hud_line);
        paint.setStyle(Paint.Style.FILL);

        if (attrs != null) {
            TypedArray typed = context.obtainStyledAttributes(attrs, R.styleable.SegmentBar);
            segmentCount = typed.getInt(R.styleable.SegmentBar_segmentCount, DEFAULT_SEGMENTS);
            typed.recycle();
        }
    }

    public void setAccent(@ColorInt int color) {
        if (accent == color) {
            return;
        }
        accent = color;
        invalidate();
    }

    /** Salto direto, sem animação (rotação de tela, atualização em background). */
    public void setProgress(float value) {
        cancelAnimator();
        progress = clamp(value);
        invalidate();
    }

    /**
     * Preenchimento progressivo. Chame depois do layout (via {@code post}): antes
     * disso a View ainda não tem largura e o desenho sai vazio.
     */
    public void animateTo(float target) {
        float end = clamp(target);
        cancelAnimator();

        long duration = HudMotion.duration(getContext(), HudMotion.SEGMENT_FILL);
        if (duration == 0L) {
            progress = end;
            invalidate();
            return;
        }

        animator = ValueAnimator.ofFloat(progress, end);
        animator.setDuration(duration);
        animator.setInterpolator(new DecelerateInterpolator(1.6f));
        animator.addUpdateListener(animation -> {
            progress = (float) animation.getAnimatedValue();
            invalidate();
        });
        animator.start();
    }

    private static float clamp(float value) {
        return Math.max(0f, Math.min(1f, value));
    }

    private void cancelAnimator() {
        if (animator != null) {
            animator.cancel();
            animator = null;
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        cancelAnimator();
        super.onDetachedFromWindow();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int width = getWidth();
        int height = getHeight();
        if (width == 0 || height == 0 || segmentCount <= 0) {
            return;
        }

        float skew = height * 0.32f;
        float segmentWidth = (width - gap * (segmentCount - 1)) / segmentCount;
        if (segmentWidth <= 0f) {
            return;
        }
        int lit = Math.round(progress * segmentCount);

        for (int index = 0; index < segmentCount; index++) {
            float x = index * (segmentWidth + gap);
            segment.reset();
            segment.moveTo(x + skew, 0f);
            segment.lineTo(x + segmentWidth, 0f);
            segment.lineTo(x + segmentWidth - skew, height);
            segment.lineTo(x, height);
            segment.close();

            paint.setColor(index < lit ? accent : offColor);
            canvas.drawPath(segment, paint);
        }
    }
}
