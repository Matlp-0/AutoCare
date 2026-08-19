package com.example.myapplication.ui.hud;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.widget.FrameLayout;

import androidx.annotation.ColorInt;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.example.myapplication.R;

/**
 * Superfície do HUD: retângulo com corte a 45° no canto superior-esquerdo e no
 * inferior-direito. A assimetria é o ponto — chanfrar os quatro cantos devolve a
 * leitura de "card".
 *
 * <p>Ordem de desenho: glow, recorte, preenchimento, filhos, hairline. O hairline
 * fica fora do recorte para não perder metade da espessura.
 *
 * <p><b>Glow.</b> Usa {@link BlurMaskFilter}, que só funciona com
 * {@code LAYER_TYPE_SOFTWARE} — caro e sem aceleração de hardware. Por isso é
 * opt-in via {@code app:glow}: ligue no painel hero e em nenhum item de
 * {@code RecyclerView}. Em tema claro o recurso {@code hud_glow_enabled} desliga
 * o efeito mesmo quando o layout pede.
 */
public class ChamferPanel extends FrameLayout {

    private final Path path = new Path();
    private final Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint hairlinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint glowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private float chamfer;
    private float hairlineWidth;
    private boolean glow;
    private boolean hairlineAccent;
    private int accent;

    public ChamferPanel(Context context) {
        this(context, null);
    }

    public ChamferPanel(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ChamferPanel(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        float density = getResources().getDisplayMetrics().density;
        chamfer = getResources().getDimension(R.dimen.hud_chamfer);
        hairlineWidth = getResources().getDimension(R.dimen.hud_hairline);
        hairlineAccent = true;

        if (attrs != null) {
            TypedArray typed = context.obtainStyledAttributes(attrs, R.styleable.ChamferPanel);
            chamfer = typed.getDimension(R.styleable.ChamferPanel_chamfer, chamfer);
            glow = typed.getBoolean(R.styleable.ChamferPanel_glow, false);
            hairlineAccent = typed.getBoolean(
                    R.styleable.ChamferPanel_hairlineAccent, true);
            typed.recycle();
        }
        glow = glow && getResources().getBoolean(R.bool.hud_glow_enabled);

        fillPaint.setStyle(Paint.Style.FILL);
        fillPaint.setColor(ContextCompat.getColor(context, R.color.hud_panel));

        hairlinePaint.setStyle(Paint.Style.STROKE);
        hairlinePaint.setStrokeWidth(hairlineWidth);

        glowPaint.setStyle(Paint.Style.STROKE);
        glowPaint.setStrokeWidth(density * 3f);

        accent = ContextCompat.getColor(context, R.color.hud_accent_cyan);
        applyAccentToPaints();

        if (glow) {
            glowPaint.setMaskFilter(new BlurMaskFilter(density * 10f, BlurMaskFilter.Blur.NORMAL));
            setLayerType(LAYER_TYPE_SOFTWARE, null);
        }
        setWillNotDraw(false);
    }

    /** Recalcula hairline e glow. Chamado por {@code applyTheme} na tela. */
    public void setAccent(@ColorInt int color) {
        if (accent == color) {
            return;
        }
        accent = color;
        applyAccentToPaints();
        invalidate();
    }

    private void applyAccentToPaints() {
        hairlinePaint.setColor(hairlineAccent
                ? accent
                : ContextCompat.getColor(getContext(), R.color.hud_line));
        glowPaint.setColor(Color.argb(90, Color.red(accent), Color.green(accent), Color.blue(accent)));
    }

    @Override
    protected void onSizeChanged(int width, int height, int oldWidth, int oldHeight) {
        super.onSizeChanged(width, height, oldWidth, oldHeight);
        buildPath(width, height);
    }

    private void buildPath(int width, int height) {
        float cut = Math.min(chamfer, Math.min(width, height) / 2f);
        path.reset();
        path.moveTo(cut, 0f);
        path.lineTo(width, 0f);
        path.lineTo(width, height - cut);
        path.lineTo(width - cut, height);
        path.lineTo(0f, height);
        path.lineTo(0f, cut);
        path.close();
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        if (path.isEmpty()) {
            buildPath(getWidth(), getHeight());
        }

        if (glow) {
            canvas.drawPath(path, glowPaint);
        }

        int checkpoint = canvas.save();
        canvas.clipPath(path);
        canvas.drawPath(path, fillPaint);
        super.dispatchDraw(canvas);
        canvas.restoreToCount(checkpoint);

        canvas.drawPath(path, hairlinePaint);
    }
}
