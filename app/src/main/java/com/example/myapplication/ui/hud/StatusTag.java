package com.example.myapplication.ui.hud;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;

import androidx.annotation.ColorInt;
import androidx.annotation.Nullable;
import androidx.core.content.res.ResourcesCompat;

import com.example.myapplication.R;

/**
 * Tag de estado em paralelogramo. Pílula arredondada é vocabulário Material e
 * briga com o resto da tela.
 *
 * <p>É o único elemento que comunica o estado de manutenção na Home — antes a mesma
 * informação aparecia três vezes (texto de status, nota de saúde e badge da lista).
 */
public class StatusTag extends View {

    private static final float SKEW_DEGREES = 14f;
    private static final int FILL_ALPHA = 28; // ~11%

    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint hairlinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path shape = new Path();
    private final Paint.FontMetrics metrics = new Paint.FontMetrics();

    private final float paddingHorizontal;
    private final float paddingVertical;
    private final float skewFactor;

    private String label = "";
    private int accent;
    private ValueAnimator pulse;

    public StatusTag(Context context) {
        this(context, null);
    }

    public StatusTag(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);

        float density = getResources().getDisplayMetrics().density;
        paddingHorizontal = 14f * density;
        paddingVertical = 7f * density;
        skewFactor = (float) Math.tan(Math.toRadians(SKEW_DEGREES));

        textPaint.setTextSize(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 11f,
                getResources().getDisplayMetrics()));
        textPaint.setTypeface(ResourcesCompat.getFont(context, R.font.barlow_condensed));
        textPaint.setLetterSpacing(0.16f);

        fillPaint.setStyle(Paint.Style.FILL);
        hairlinePaint.setStyle(Paint.Style.STROKE);
        hairlinePaint.setStrokeWidth(getResources().getDimension(R.dimen.hud_hairline));

        accent = androidx.core.content.ContextCompat.getColor(context, R.color.hud_accent_cyan);
        applyAccent();
    }

    public void setAccent(@ColorInt int color, String label) {
        String upper = label == null ? "" : label.toUpperCase();
        boolean sizeChanged = !upper.equals(this.label);
        this.label = upper;
        this.accent = color;
        applyAccent();
        setContentDescription(upper);
        if (sizeChanged) {
            requestLayout();
        }
        invalidate();
    }

    /** Um pulso, nunca em laço: brilho piscando sem parar vira ruído. */
    public void pulseOnce() {
        long duration = HudMotion.duration(getContext(), HudMotion.TAG_PULSE);
        if (duration == 0L) {
            return;
        }
        if (pulse != null) {
            pulse.cancel();
        }
        pulse = ValueAnimator.ofFloat(1f, 0.35f, 1f);
        pulse.setDuration(duration);
        pulse.addUpdateListener(animation -> setAlpha((float) animation.getAnimatedValue()));
        pulse.start();
    }

    @Override
    protected void onDetachedFromWindow() {
        if (pulse != null) {
            pulse.cancel();
            pulse = null;
        }
        super.onDetachedFromWindow();
    }

    private void applyAccent() {
        textPaint.setColor(accent);
        hairlinePaint.setColor(accent);
        fillPaint.setColor(Color.argb(FILL_ALPHA,
                Color.red(accent), Color.green(accent), Color.blue(accent)));
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        textPaint.getFontMetrics(metrics);
        float textHeight = metrics.descent - metrics.ascent;
        int height = Math.round(textHeight + paddingVertical * 2f);
        float skew = height * skewFactor;
        int width = Math.round(textPaint.measureText(label) + paddingHorizontal * 2f + skew);

        setMeasuredDimension(
                resolveSize(width, widthMeasureSpec),
                resolveSize(height, heightMeasureSpec));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int width = getWidth();
        int height = getHeight();
        if (width == 0 || height == 0) {
            return;
        }

        float skew = height * skewFactor;
        shape.reset();
        shape.moveTo(skew, 0f);
        shape.lineTo(width, 0f);
        shape.lineTo(width - skew, height);
        shape.lineTo(0f, height);
        shape.close();

        canvas.drawPath(shape, fillPaint);
        canvas.drawPath(shape, hairlinePaint);

        textPaint.getFontMetrics(metrics);
        float baseline = (height - (metrics.descent + metrics.ascent)) / 2f;
        canvas.drawText(label, skew / 2f + paddingHorizontal, baseline, textPaint);
    }
}
