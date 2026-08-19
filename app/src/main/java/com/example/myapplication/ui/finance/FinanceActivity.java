package com.example.myapplication.ui.finance;

import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import com.example.myapplication.R;
import com.example.myapplication.data.local.entity.Vehicle;
import com.example.myapplication.domain.finance.CostSummary;
import com.example.myapplication.ui.carbon.CarbonSystemBars;
import com.example.myapplication.ui.carbon.CarbonTechnicalRow;
import com.example.myapplication.util.DateUtils;
import com.example.myapplication.util.Formatters;
import com.google.android.material.appbar.MaterialToolbar;

/** Painel financeiro do veículo ativo: custo por km, composição e mês a mês. */
public class FinanceActivity extends AppCompatActivity {

    /** Quantas categorias aparecem na lista; o resto vira "Outros". */
    private static final int TOP_CATEGORIES = 6;

    private TextView textVehicle;
    private TextView textCostPerKm;
    private TextView textBase;
    private TextView textMonthlyPeak;
    private TextView textEmpty;
    private View barMaintenance;
    private View barFuel;
    private CarbonTechnicalRow rowMaintenance;
    private CarbonTechnicalRow rowFuel;
    private CarbonTechnicalRow rowTotal;
    private CarbonTechnicalRow rowMonthlyAverage;
    private CarbonTechnicalRow rowCurrentMonth;
    private CarbonTechnicalRow rowProjection;
    private MonthlyBarChart chartMonthly;
    private LinearLayout groupCategories;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_finance);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(view -> finish());

        textVehicle = findViewById(R.id.textVehicle);
        textCostPerKm = findViewById(R.id.textCostPerKm);
        textBase = findViewById(R.id.textBase);
        textMonthlyPeak = findViewById(R.id.textMonthlyPeak);
        textEmpty = findViewById(R.id.textEmpty);
        barMaintenance = findViewById(R.id.barMaintenance);
        barFuel = findViewById(R.id.barFuel);
        rowMaintenance = findViewById(R.id.rowMaintenance);
        rowFuel = findViewById(R.id.rowFuel);
        rowTotal = findViewById(R.id.rowTotal);
        rowMonthlyAverage = findViewById(R.id.rowMonthlyAverage);
        rowCurrentMonth = findViewById(R.id.rowCurrentMonth);
        rowProjection = findViewById(R.id.rowProjection);
        chartMonthly = findViewById(R.id.chartMonthly);
        groupCategories = findViewById(R.id.groupCategories);

        FinanceViewModel viewModel = new ViewModelProvider(this).get(FinanceViewModel.class);
        viewModel.vehicle().observe(this, new Observer<Vehicle>() {
            @Override
            public void onChanged(Vehicle vehicle) {
                textVehicle.setText(vehicle == null ? "" : vehicle.displayName());
            }
        });
        viewModel.summary().observe(this, new Observer<CostSummary>() {
            @Override
            public void onChanged(CostSummary summary) {
                bind(summary);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        CarbonSystemBars.apply(this);
    }

    private void bind(CostSummary summary) {
        boolean empty = summary == null || summary.totalSpent() <= 0d;
        textEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
        if (summary == null) {
            return;
        }

        textCostPerKm.setText(summary.hasCostPerKm()
                ? Formatters.costPerKm(summary.costPerKm) : getString(R.string.fuel_dash));

        bindComposition(summary);

        rowTotal.setLabel(getString(R.string.finance_total));
        rowTotal.setValue(Formatters.money(summary.totalSpent()));

        rowMonthlyAverage.setLabel(getString(R.string.finance_monthly_average));
        rowMonthlyAverage.setValue(Formatters.money(summary.monthlyAverage));

        rowCurrentMonth.setLabel(getString(R.string.finance_current_month));
        rowCurrentMonth.setValue(Formatters.money(summary.currentMonthSpent));

        rowProjection.setLabel(getString(R.string.finance_projection));
        if (summary.projectedYearlyCost > 0d) {
            rowProjection.setValue(Formatters.money(summary.projectedYearlyCost));
            rowProjection.setValueColor(R.color.carbon_cyan);
        } else {
            rowProjection.setValue(getString(R.string.fuel_dash));
            rowProjection.setValueColor(R.color.carbon_text_primary);
        }

        bindBase(summary);

        chartMonthly.submit(summary.months, summary.maxMonthTotal());
        double peak = summary.maxMonthTotal();
        textMonthlyPeak.setText(peak > 0d
                ? getString(R.string.finance_monthly_peak, Formatters.money(peak)) : "");

        bindCategories(summary);
    }

    private void bindComposition(CostSummary summary) {
        rowMaintenance.setLabel(getString(R.string.finance_maintenance));
        rowMaintenance.setValue(Formatters.money(summary.maintenanceSpent));
        rowFuel.setLabel(getString(R.string.finance_fuel));
        rowFuel.setValue(Formatters.money(summary.fuelSpent));

        // A barra é a própria proporção do gasto: peso 0 some, sem cálculo extra.
        float fuelShare = (float) summary.fuelShare();
        setWeight(barMaintenance, 1f - fuelShare);
        setWeight(barFuel, fuelShare);
    }

    private void setWeight(View view, float weight) {
        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) view.getLayoutParams();
        params.weight = Math.max(0f, weight);
        view.setLayoutParams(params);
    }

    private void bindBase(CostSummary summary) {
        if (summary.coveredKm <= 0 || summary.firstRecordDate <= 0) {
            textBase.setText(R.string.finance_base_empty);
            return;
        }
        textBase.setText(getString(R.string.finance_base,
                Formatters.km(summary.coveredKm),
                DateUtils.formatShort(summary.firstRecordDate),
                DateUtils.formatShort(summary.lastRecordDate),
                summary.maintenanceCount,
                summary.refuelCount));
    }

    private void bindCategories(CostSummary summary) {
        groupCategories.removeAllViews();
        int limit = Math.min(TOP_CATEGORIES, summary.categories.size());
        double listed = 0d;

        for (int index = 0; index < limit; index++) {
            CostSummary.CategoryCost category = summary.categories.get(index);
            listed += category.amount;
            groupCategories.addView(categoryRow(category.label,
                    Formatters.money(category.amount), (int) Math.round(category.share * 100d)));
        }

        double rest = summary.maintenanceSpent - listed;
        if (rest > 0.01d) {
            int share = summary.maintenanceSpent > 0d
                    ? (int) Math.round(rest / summary.maintenanceSpent * 100d) : 0;
            groupCategories.addView(categoryRow(getString(R.string.carbon_others_label),
                    Formatters.money(rest), share));
        }
    }

    private View categoryRow(String label, String amount, int sharePercent) {
        CarbonTechnicalRow row = new CarbonTechnicalRow(this);
        row.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        row.setLabel(getString(R.string.finance_category_value, label, sharePercent));
        row.setValue(amount);
        return row;
    }
}
