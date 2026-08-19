package com.example.myapplication.ui.fuel;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.AppContainer;
import com.example.myapplication.AutoCareApp;
import com.example.myapplication.R;
import com.example.myapplication.data.local.entity.Refuel;
import com.example.myapplication.data.local.entity.Vehicle;
import com.example.myapplication.domain.fuel.FuelStats;
import com.example.myapplication.domain.fuel.UsageEstimator;
import com.example.myapplication.domain.model.DashboardState;
import com.example.myapplication.ui.carbon.CarbonSystemBars;
import com.example.myapplication.ui.carbon.CarbonTechnicalRow;
import com.example.myapplication.ui.carbon.KmUpdateDialog;
import com.example.myapplication.ui.main.VehicleViewModel;
import com.example.myapplication.util.DateUtils;
import com.example.myapplication.util.Formatters;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.List;

/**
 * Aba Combustível: consumo, custo e ritmo de uso do veículo.
 *
 * <p>Os números vêm prontos do {@code VehicleViewModel} (calculados por
 * {@code FuelStatsCalculator} e {@code UsageEstimator}); aqui só formatamos.
 */
public class FuelFragment extends Fragment {

    private AppContainer container;
    private VehicleViewModel viewModel;
    private RefuelFormViewModel formViewModel;
    private RefuelAdapter adapter;

    private TextView textConsumption;
    private TextView textUsageHint;
    private TextView textEmptyFuel;
    private CarbonTechnicalRow rowLastConsumption;
    private CarbonTechnicalRow rowRange;
    private CarbonTechnicalRow rowCostPerKm;
    private CarbonTechnicalRow rowSpent30;
    private CarbonTechnicalRow rowPricePerLiter;
    private CarbonTechnicalRow rowKmPerDay;
    private CarbonTechnicalRow rowKmPerMonth;
    private CarbonTechnicalRow rowCurrentKm;

    private final List<Refuel> refuels = new ArrayList<>();
    private Vehicle vehicle;
    private boolean firstRender = true;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_fuel, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        container = AutoCareApp.container(requireContext());

        textConsumption = view.findViewById(R.id.textConsumption);
        textUsageHint = view.findViewById(R.id.textUsageHint);
        textEmptyFuel = view.findViewById(R.id.textEmptyFuel);
        rowLastConsumption = view.findViewById(R.id.rowLastConsumption);
        rowRange = view.findViewById(R.id.rowRange);
        rowCostPerKm = view.findViewById(R.id.rowCostPerKm);
        rowSpent30 = view.findViewById(R.id.rowSpent30);
        rowPricePerLiter = view.findViewById(R.id.rowPricePerLiter);
        rowKmPerDay = view.findViewById(R.id.rowKmPerDay);
        rowKmPerMonth = view.findViewById(R.id.rowKmPerMonth);
        rowCurrentKm = view.findViewById(R.id.rowCurrentKm);

        view.findViewById(R.id.buttonAddRefuel).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View clicked) {
                startActivity(new Intent(requireContext(), AddRefuelActivity.class));
            }
        });
        view.findViewById(R.id.buttonUpdateKm).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View clicked) {
                KmUpdateDialog.show(requireContext(), viewModel,
                        vehicle == null ? 0 : vehicle.currentKm);
            }
        });

        adapter = new RefuelAdapter(new RefuelAdapter.Listener() {
            @Override
            public void onClick(Refuel refuel) {
                openEditor(refuel);
            }

            @Override
            public void onLongClick(Refuel refuel) {
                showQuickActions(refuel);
            }
        });
        RecyclerView recycler = view.findViewById(R.id.recyclerRefuels);
        recycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        recycler.setAdapter(adapter);

        viewModel = new ViewModelProvider(requireActivity()).get(VehicleViewModel.class);
        formViewModel = new ViewModelProvider(this).get(RefuelFormViewModel.class);
        formViewModel.deleted().observe(getViewLifecycleOwner(), new Observer<Boolean>() {
            @Override
            public void onChanged(Boolean success) {
                if (Boolean.TRUE.equals(success)) {
                    Toast.makeText(requireContext(), R.string.fuel_deleted,
                            Toast.LENGTH_SHORT).show();
                }
            }
        });

        viewModel.vehicle().observe(getViewLifecycleOwner(), new Observer<Vehicle>() {
            @Override
            public void onChanged(Vehicle active) {
                vehicle = active;
            }
        });
        viewModel.refuels().observe(getViewLifecycleOwner(), new Observer<List<Refuel>>() {
            @Override
            public void onChanged(List<Refuel> items) {
                refuels.clear();
                if (items != null) {
                    refuels.addAll(items);
                }
                adapter.submit(refuels,
                        FuelFragment.this.container.fuelStatsCalculator.consumptionByRefuel(refuels));
                textEmptyFuel.setVisibility(refuels.isEmpty() ? View.VISIBLE : View.GONE);
            }
        });
        viewModel.dashboard().observe(getViewLifecycleOwner(), new Observer<DashboardState>() {
            @Override
            public void onChanged(DashboardState state) {
                bind(state);
                if (firstRender) {
                    firstRender = false;
                    animateIn();
                }
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        CarbonSystemBars.applyOnResume(getActivity(), getView());
    }

    private void bind(DashboardState state) {
        FuelStats fuel = state == null ? new FuelStats() : state.fuel;
        UsageEstimator.Usage usage = state == null
                ? new UsageEstimator.Usage() : state.usage;
        int currentKm = state == null || state.vehicle == null ? 0 : state.vehicle.currentKm;

        textConsumption.setText(fuel.hasConsumption()
                ? Formatters.consumption(fuel.averageConsumption)
                : getString(R.string.fuel_no_data));

        rowLastConsumption.setLabel(getString(R.string.fuel_last_consumption));
        rowLastConsumption.setValue(Formatters.consumption(fuel.lastConsumption));

        int range = fuel.estimatedRangeKm(currentKm);
        rowRange.setLabel(getString(R.string.fuel_range));
        rowRange.setValue(range > 0 ? Formatters.km(range) : getString(R.string.fuel_dash));
        rowRange.setValueColor(range > 0 ? R.color.carbon_cyan : R.color.carbon_text_primary);

        rowCostPerKm.setLabel(getString(R.string.fuel_cost_per_km));
        rowCostPerKm.setValue(Formatters.costPerKm(fuel.costPerKm));

        rowSpent30.setLabel(getString(R.string.fuel_spent_30));
        rowSpent30.setValue(Formatters.money(fuel.spentLast30Days));

        rowPricePerLiter.setLabel(getString(R.string.fuel_average_price));
        rowPricePerLiter.setValue(Formatters.pricePerLiter(fuel.averagePricePerLiter));

        rowKmPerDay.setLabel(getString(R.string.fuel_km_per_day));
        rowKmPerDay.setValue(usage.reliable
                ? Formatters.kmPerDay(usage.kmPerDay) : getString(R.string.fuel_dash));

        rowKmPerMonth.setLabel(getString(R.string.fuel_km_per_month));
        rowKmPerMonth.setValue(usage.reliable
                ? Formatters.km(usage.kmPerMonth()) : getString(R.string.fuel_dash));

        rowCurrentKm.setLabel(getString(R.string.fuel_last_reading));
        rowCurrentKm.setValue(usage.lastDate > 0
                ? getString(R.string.fuel_reading_value, Formatters.km(usage.lastKm),
                        DateUtils.formatShort(usage.lastDate))
                : Formatters.km(currentKm));

        textUsageHint.setText(usage.reliable
                ? getString(R.string.fuel_usage_ready, usage.readingCount)
                : getString(R.string.fuel_usage_pending));
    }

    private void openEditor(Refuel refuel) {
        Intent intent = new Intent(requireContext(), AddRefuelActivity.class);
        intent.putExtra(AddRefuelActivity.EXTRA_REFUEL_ID, refuel.id);
        startActivity(intent);
    }

    private void showQuickActions(final Refuel refuel) {
        String[] options = {getString(R.string.action_edit), getString(R.string.action_delete)};
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(DateUtils.formatHistory(refuel.date))
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        openEditor(refuel);
                    } else {
                        confirmDelete(refuel);
                    }
                })
                .show();
    }

    private void confirmDelete(final Refuel refuel) {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.delete_confirm_title)
                .setMessage(R.string.fuel_delete_message)
                .setPositiveButton(R.string.action_delete, (dialog, which) ->
                        formViewModel.delete(refuel.id))
                .setNegativeButton(R.string.carbon_cancel, null)
                .show();
    }

    /** Mesma entrada discreta das outras abas. */
    private void animateIn() {
        View root = getView();
        if (root == null) {
            return;
        }
        root.setAlpha(0f);
        root.setTranslationX(getResources().getDimension(R.dimen.carbon_shift));
        root.animate()
                .alpha(1f)
                .translationX(0f)
                .setDuration(getResources().getInteger(R.integer.carbon_duration_state))
                .setInterpolator(new DecelerateInterpolator())
                .start();
    }
}
