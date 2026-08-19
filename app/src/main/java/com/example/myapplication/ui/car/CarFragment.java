package com.example.myapplication.ui.car;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import com.example.myapplication.AutoCareApp;
import com.example.myapplication.R;
import com.example.myapplication.data.local.entity.Vehicle;
import com.example.myapplication.domain.manual.ManualInfo;
import com.example.myapplication.domain.model.DashboardState;
import com.example.myapplication.domain.model.UpcomingMaintenance;
import com.example.myapplication.ui.carbon.CarbonFilterTabs;
import com.example.myapplication.ui.carbon.CarbonSystemBars;
import com.example.myapplication.ui.carbon.CarbonTechnicalRow;
import com.example.myapplication.ui.finance.FinanceActivity;
import com.example.myapplication.ui.carbon.CarbonTheme;
import com.example.myapplication.ui.main.VehicleViewModel;
import com.example.myapplication.ui.onboarding.VehicleFormActivity;
import com.example.myapplication.util.AppPreferences;
import com.example.myapplication.util.Callback;
import com.example.myapplication.util.DateUtils;
import com.example.myapplication.util.Formatters;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.net.URI;

/**
 * Carro no Carbon UI: ficha técnica do veículo.
 *
 * <p>Exibe apenas o que existe no cadastro — nenhum campo é inventado — e mantém
 * os mesmos fluxos de editar dados, atualizar quilometragem e buscar o plano.
 */
public class CarFragment extends Fragment {

    private VehicleViewModel viewModel;
    private DashboardState current;

    private TextView textVehicleName;
    private TextView textVehicleYear;
    private TextView textVehicleEngine;
    private TextView textVehicleKm;
    private TextView textPlanSource;
    private CarbonTechnicalRow rowBrand;
    private CarbonTechnicalRow rowModel;
    private CarbonTechnicalRow rowYear;
    private CarbonTechnicalRow rowEngine;
    private CarbonTechnicalRow rowTransmission;
    private CarbonTechnicalRow rowFuel;
    private CarbonTechnicalRow rowLastMaintenance;
    private CarbonTechnicalRow rowNextRevision;
    private CarbonTechnicalRow rowTotalSpent;
    private CarbonTechnicalRow rowMaintenanceCount;

    private boolean firstRender = true;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_car, container, false);
    }

    @Override
    public void onResume() {
        super.onResume();
        CarbonSystemBars.applyOnResume(getActivity(), getView());
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        textVehicleName = view.findViewById(R.id.textVehicleName);
        textVehicleYear = view.findViewById(R.id.textVehicleYear);
        textVehicleEngine = view.findViewById(R.id.textVehicleEngine);
        textVehicleKm = view.findViewById(R.id.textVehicleKm);
        textPlanSource = view.findViewById(R.id.textPlanSource);
        rowBrand = view.findViewById(R.id.rowBrand);
        rowModel = view.findViewById(R.id.rowModel);
        rowYear = view.findViewById(R.id.rowYear);
        rowEngine = view.findViewById(R.id.rowEngine);
        rowTransmission = view.findViewById(R.id.rowTransmission);
        rowFuel = view.findViewById(R.id.rowFuel);
        rowLastMaintenance = view.findViewById(R.id.rowLastMaintenance);
        rowNextRevision = view.findViewById(R.id.rowNextRevision);
        rowTotalSpent = view.findViewById(R.id.rowTotalSpent);
        rowMaintenanceCount = view.findViewById(R.id.rowMaintenanceCount);

        viewModel = new ViewModelProvider(requireActivity()).get(VehicleViewModel.class);
        viewModel.dashboard().observe(getViewLifecycleOwner(), new Observer<DashboardState>() {
            @Override
            public void onChanged(DashboardState state) {
                current = state;
                render(state);
            }
        });

        view.findViewById(R.id.buttonEdit).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View clicked) {
                if (current == null || current.vehicle == null) {
                    return;
                }
                Intent intent = new Intent(requireContext(), VehicleFormActivity.class);
                intent.putExtra(VehicleFormActivity.EXTRA_VEHICLE_ID, current.vehicle.id);
                startActivity(intent);
            }
        });

        view.findViewById(R.id.buttonFinance).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View clicked) {
                startActivity(new Intent(requireContext(), FinanceActivity.class));
            }
        });

        view.findViewById(R.id.buttonUpdateKm).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View clicked) {
                showUpdateKmDialog();
            }
        });

        final TextView updatePlan = view.findViewById(R.id.buttonUpdatePlan);
        updatePlan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View clicked) {
                if (current == null || current.vehicle == null) {
                    return;
                }
                updatePlan.setEnabled(false);
                textPlanSource.setText(R.string.identification_searching);
                AutoCareApp.container(requireContext()).planRepository
                        .refreshFromWeb(current.vehicle, new Callback<ManualInfo>() {
                            @Override
                            public void onResult(ManualInfo info) {
                                updatePlan.setEnabled(true);
                                if (info == null || info.plan.isEmpty()) {
                                    textPlanSource.setText(R.string.plan_not_found);
                                    return;
                                }
                                textPlanSource.setText(info.manualUrl != null
                                        ? getString(R.string.identification_source_web,
                                                host(info.manualUrl))
                                        : "");
                                Toast.makeText(requireContext(), R.string.plan_updated,
                                        Toast.LENGTH_SHORT).show();
                                viewModel.refreshPlan();
                            }
                        });
            }
        });

        setupThemeTabs(view);
    }

    /** Aparência do app inteiro: sistema, claro ou escuro. */
    private void setupThemeTabs(View view) {
        CarbonFilterTabs themeTabs = view.findViewById(R.id.themeTabs);
        themeTabs.setTabs(getString(R.string.carbon_theme_system),
                getString(R.string.carbon_theme_light),
                getString(R.string.carbon_theme_dark));

        final AppPreferences preferences =
                AutoCareApp.container(requireContext()).preferences;
        themeTabs.select(preferences.getThemeMode(), false);
        themeTabs.setOnTabSelected(new CarbonFilterTabs.OnTabSelected() {
            @Override
            public void onTabSelected(int index) {
                if (index == preferences.getThemeMode()) {
                    return;
                }
                preferences.setThemeMode(index);
                // O AppCompat recria a Activity já com o novo modo.
                CarbonTheme.apply(index);
            }
        });
    }

    private String host(String url) {
        if (url == null) {
            return "";
        }
        try {
            String host = URI.create(url).getHost();
            return host == null ? url : host.replaceFirst("^www\\.", "");
        } catch (IllegalArgumentException error) {
            return url;
        }
    }

    private void render(DashboardState state) {
        if (state == null || state.vehicle == null) {
            return;
        }
        Vehicle vehicle = state.vehicle;
        textVehicleName.setText(vehicle.displayName());
        textVehicleYear.setText(String.valueOf(vehicle.year));
        textVehicleEngine.setText(vehicle.engine);
        textVehicleKm.setText(Formatters.km(vehicle.currentKm));

        rowBrand.setValue(vehicle.brand);
        rowModel.setValue(vehicle.model);
        rowYear.setValue(String.valueOf(vehicle.year));
        rowEngine.setValue(vehicle.engine);
        rowTransmission.setValue(vehicle.transmission);
        rowFuel.setValue(vehicle.fuel);

        if (state.lastMaintenance != null) {
            rowLastMaintenance.setValue(DateUtils.formatShort(state.lastMaintenance.date) + " • "
                    + Formatters.km(state.lastMaintenance.odometerKm));
        } else {
            rowLastMaintenance.setValue("—");
        }

        UpcomingMaintenance next = state.schedule.next;
        rowNextRevision.setValue(next != null
                ? next.label + " • " + next.remainingText()
                : "—");

        rowTotalSpent.setValue(Formatters.money(state.totalSpent));
        rowTotalSpent.setValueColor(R.color.carbon_cyan);
        rowMaintenanceCount.setValue(String.valueOf(state.maintenanceCount));

        if (firstRender) {
            firstRender = false;
            animateIn();
        }
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

    private void showUpdateKmDialog() {
        if (current == null || current.vehicle == null) {
            return;
        }
        final EditText input = new EditText(requireContext());
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        input.setText(String.valueOf(current.vehicle.currentKm));

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.car_update_km)
                .setView(input)
                .setPositiveButton(R.string.action_save, (dialog, which) -> {
                    String value = input.getText().toString().trim();
                    if (value.isEmpty()) {
                        return;
                    }
                    try {
                        int km = Integer.parseInt(value);
                        if (km < 0) {
                            throw new NumberFormatException("negativo");
                        }
                        viewModel.updateKm(km);
                    } catch (NumberFormatException error) {
                        Toast.makeText(requireContext(), R.string.error_km_invalid,
                                Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton(R.string.carbon_cancel, null)
                .show();
    }
}
