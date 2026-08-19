package com.example.myapplication.ui.finance;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.example.myapplication.R;
import com.example.myapplication.domain.finance.CostSummary;

import java.util.ArrayList;
import java.util.List;

/**
 * Barras do gasto mensal, empilhando manutenção e combustível.
 *
 * <p>Desenhada à mão para manter a linguagem do Carbon UI (retângulos retos, sem
 * eixo decorativo) e para não trazer biblioteca de gráfico só por isto.
 */
public class MonthlyBarChart extends View {

    private static final String[] MONTH_INITIALS = {
            "J", "F", "M", "A", "M", "J", "J", "A", "S", "O", "N", "D"};

    private final List<CostSummary.MonthCost> months = new ArrayList<>();
    private final Paint maintenancePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint fuelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint basePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint labelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private double maxValue;

    public MonthlyBarChart(Context context) {
        this(context, null);
    }

    public MonthlyBarChart(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        maintenancePaint.setColor(ContextCompat.getColor(context, R.color.carbon_accent));
        fuelPaint.setColor(ContextCompat.getColor(context, R.color.carbon_cyan));
        basePaint.setColor(ContextCompat.getColor(context, R.color.carbon_line));
        labelPaint.setColor(ContextCompat.getColor(context, R.color.carbon_text_dim));
        labelPaint.setTextSize(getResources().getDisplayMetrics().density * 10f);
        labelPaint.setTextAlign(Paint.Align.CENTER);
    }

    public void submit(List<CostSummary.MonthCost> data, double max) {
        months.clear();
        if (data != null) {
            months.addAll(data);
        }
        maxValue = max;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (months.isEmpty()) {
            return;
        }

        float density = getResources().getDisplayMetrics().density;
        float labelHeight = density * 16f;
        float chartHeight = getHeight() - labelHeight;
        float slot = getWidth() / (float) months.size();
        float barWidth = Math.max(density * 6f, slot - density * 6f);

        for (int index = 0; index < months.size(); index++) {
            CostSummary.MonthCost month = months.get(index);
            float centerX = slot * index + slot / 2f;
            float left = centerX - barWidth / 2f;
            float right = centerX + barWidth / 2f;

            // Régua de base: mês sem gasto continua ocupando lugar na série.
            canvas.drawRect(left, chartHeight - density, right, chartHeight, basePaint);

            if (maxValue > 0d && month.total() > 0d) {
                float fullHeight = (float) (month.total() / maxValue) * (chartHeight - density * 4f);
                float fuelHeight = month.total() > 0d
                        ? (float) (month.fuel / month.total()) * fullHeight : 0f;

                float top = chartHeight - fullHeight;
                canvas.drawRect(left, top, right, chartHeight - fuelHeight, maintenancePaint);
                canvas.drawRect(left, chartHeight - fuelHeight, right, chartHeight, fuelPaint);
            }

            canvas.drawText(MONTH_INITIALS[(month.month - 1) % 12], centerX,
                    getHeight() - density * 3f, labelPaint);
        }
    }
}
