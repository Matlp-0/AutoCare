package com.example.myapplication.ui.home;

import android.animation.ValueAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.data.local.entity.Vehicle;
import com.example.myapplication.domain.model.DashboardState;
import com.example.myapplication.domain.model.UpcomingMaintenance;
import com.example.myapplication.ui.carbon.CarbonSystemBars;
import com.example.myapplication.ui.carbon.KmUpdateDialog;
import com.example.myapplication.ui.hud.ChamferPanel;
import com.example.myapplication.ui.hud.HudMotion;
import com.example.myapplication.ui.hud.SegmentBar;
import com.example.myapplication.ui.hud.StatusTag;
import com.example.myapplication.ui.hud.StatusTheme;
import com.example.myapplication.ui.main.MainActivity;
import com.example.myapplication.ui.main.VehicleViewModel;
import com.example.myapplication.ui.maintenance.AddMaintenanceActivity;
import com.example.myapplication.util.Formatters;

import java.util.List;

/**
 * Home no tema HUD automotivo.
 *
 * <p>Só a apresentação mudou: os dados continuam vindo de {@code DashboardState} e
 * nenhuma regra de negócio, cálculo de saúde ou consulta é feita aqui.
 *
 * <p>A cor de acento não é escolhida por nenhum componente: {@link #applyTheme}
 * recebe um {@link StatusTheme} e propaga a mesma cor para o painel hero, o
 * medidor, a tag, o odômetro e os controles de ação.
 */
public class HomeFragment extends Fragment {

    // Três itens comunicam melhor para quem está começando; o resto fica na aba.
    private static final int UPCOMING_LIMIT = 3;

    /**
     * A sequência de entrada roda uma vez por sessão do processo. Repetir a cada
     * troca de aba irrita.
     */
    private static boolean introPlayed;

    private final UpcomingAdapter adapter = new UpcomingAdapter();

    private ChamferPanel heroPanel;
    private TextView textVehicleName;
    private TextView textVehicleSpecs;
    private TextView textOdometer;
    private View odometerRow;
    private SegmentBar segmentBar;
    private StatusTag statusTag;
    private TextView textHealthLabel;
    private TextView textHealthReasons;
    private TextView textNextTitle;
    private TextView textNextRemaining;
    private TextView textHistoryCount;
    private TextView textEmpty;
    private View groupFirstSteps;
    private TextView buttonSeeAll;
    private TextView buttonDetails;
    private TextView textStepPlan;

    private VehicleViewModel viewModel;
    private DashboardState currentState;
    private ValueAnimator odometerAnimator;
    private boolean firstRender = true;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        heroPanel = view.findViewById(R.id.heroPanel);
        textVehicleName = view.findViewById(R.id.textVehicleName);
        textVehicleSpecs = view.findViewById(R.id.textVehicleSpecs);
        textOdometer = view.findViewById(R.id.textOdometer);
        odometerRow = view.findViewById(R.id.odometerRow);
        segmentBar = view.findViewById(R.id.segmentBar);
        statusTag = view.findViewById(R.id.statusTag);
        textHealthLabel = view.findViewById(R.id.textHealthLabel);
        textHealthReasons = view.findViewById(R.id.textHealthReasons);
        textNextTitle = view.findViewById(R.id.textNextTitle);
        textNextRemaining = view.findViewById(R.id.textNextRemaining);
        textHistoryCount = view.findViewById(R.id.textHistoryCount);
        textEmpty = view.findViewById(R.id.textEmpty);
        groupFirstSteps = view.findViewById(R.id.groupFirstSteps);
        buttonSeeAll = view.findViewById(R.id.buttonSeeAll);
        buttonDetails = view.findViewById(R.id.buttonDetails);
        textStepPlan = view.findViewById(R.id.textStepPlan);

        RecyclerView recycler = view.findViewById(R.id.recyclerUpcoming);
        recycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        recycler.setAdapter(adapter);

        buttonSeeAll.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View clicked) {
                if (getActivity() instanceof MainActivity) {
                    ((MainActivity) getActivity()).openMaintenanceTab();
                }
            }
        });

        // Mesma navegação de antes: "Detalhes" leva para a aba Revisões.
        buttonDetails.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View clicked) {
                if (getActivity() instanceof MainActivity) {
                    ((MainActivity) getActivity()).openScheduleTab();
                }
            }
        });

        viewModel = new ViewModelProvider(requireActivity()).get(VehicleViewModel.class);

        // Atualizar a km é a ação mais frequente: fica a um toque na Home.
        odometerRow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View clicked) {
                if (currentState != null && currentState.vehicle != null) {
                    KmUpdateDialog.show(requireContext(), viewModel,
                            currentState.vehicle.currentKm);
                }
            }
        });

        view.findViewById(R.id.buttonFirstAction).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View clicked) {
                startActivity(new Intent(requireContext(), AddMaintenanceActivity.class));
            }
        });

        viewModel.dashboard().observe(getViewLifecycleOwner(), new Observer<DashboardState>() {
            @Override
            public void onChanged(DashboardState state) {
                render(state);
            }
        });
    }

    /** Barras do sistema seguem o tema atual (claro ou escuro). */
    @Override
    public void onResume() {
        super.onResume();
        CarbonSystemBars.applyOnResume(getActivity(), getView());
    }

    @Override
    public void onDestroyView() {
        if (odometerAnimator != null) {
            odometerAnimator.cancel();
            odometerAnimator = null;
        }
        super.onDestroyView();
    }

    private void render(DashboardState state) {
        if (state == null || state.vehicle == null) {
            textVehicleName.setText(R.string.app_name);
            textEmpty.setVisibility(View.VISIBLE);
            return;
        }
        currentState = state;
        Vehicle vehicle = state.vehicle;

        // O acento vem do estado calculado pelo domínio e reveste a tela inteira.
        applyTheme(StatusTheme.from(state.status()));

        textVehicleName.setText(vehicle.displayName());
        textVehicleSpecs.setText(vehicle.displaySpecs());

        boolean playIntro = firstRender && !introPlayed;
        if (!playIntro) {
            setOdometer(vehicle.currentKm);
        }
        odometerRow.setContentDescription(getString(R.string.hud_odometer_desc,
                Formatters.km(vehicle.currentKm)));

        int score = state.health.score;
        textHealthLabel.setText(score > 0
                ? getString(R.string.hud_health, score)
                : getString(R.string.hud_health_empty));
        textHealthReasons.setText(TextUtils.join(" • ", state.health.reasons));

        UpcomingMaintenance next = state.schedule.next;
        if (next != null) {
            textNextTitle.setText(next.label);
            textNextRemaining.setText(next.remainingDetailed());
        } else {
            textNextTitle.setText(R.string.home_next_none);
            textNextRemaining.setText("");
        }

        textHistoryCount.setText(getString(R.string.carbon_history_count,
                state.maintenanceCount));

        boolean novice = state.maintenanceCount == 0;
        groupFirstSteps.setVisibility(novice ? View.VISIBLE : View.GONE);
        textStepPlan.setText(state.timeline.isEmpty()
                ? R.string.carbon_step_plan_missing
                : R.string.carbon_step_plan_ok);

        List<UpcomingMaintenance> upcoming = state.schedule.all;
        int limit = Math.min(UPCOMING_LIMIT, upcoming.size());
        adapter.submit(upcoming.subList(0, limit));
        buttonSeeAll.setVisibility(upcoming.size() > limit ? View.VISIBLE : View.GONE);
        textEmpty.setVisibility(upcoming.isEmpty() ? View.VISIBLE : View.GONE);

        float health = Math.max(0, Math.min(100, score)) / 100f;
        if (playIntro) {
            introPlayed = true;
            playIntro(vehicle.currentKm, health);
        } else if (firstRender) {
            // A View ainda não tem largura no primeiro render: medir depois do layout.
            final float target = health;
            segmentBar.post(new Runnable() {
                @Override
                public void run() {
                    segmentBar.animateTo(target);
                }
            });
        } else {
            segmentBar.animateTo(health);
        }
        firstRender = false;
    }

    /**
     * Ponto único de propagação do acento. Nenhum componente resolve a cor sozinho —
     * trocar a faixa de km restantes repinta a tela inteira sem nenhuma outra
     * alteração de código.
     */
    private void applyTheme(StatusTheme next) {
        int accent = next.color(requireContext());

        heroPanel.setAccent(accent);
        segmentBar.setAccent(accent);
        statusTag.setAccent(accent, next.label(requireContext()));
        textOdometer.setTextColor(accent);
        textNextRemaining.setTextColor(accent);
        buttonDetails.setTextColor(accent);
        buttonSeeAll.setTextColor(accent);
    }

    private void setOdometer(int km) {
        if (odometerAnimator != null) {
            odometerAnimator.cancel();
            odometerAnimator = null;
        }
        textOdometer.setText(Formatters.kmNumber(km));
    }

    /**
     * Sequência de entrada encadeada (~950ms): o painel entra pela direita, o
     * odômetro conta a partir de zero, o medidor preenche e a tag pulsa uma vez.
     * Com animações desligadas no sistema tudo aparece no estado final.
     */
    private void playIntro(final int km, final float health) {
        if (!HudMotion.enabled(requireContext())) {
            setOdometer(km);
            segmentBar.setProgress(health);
            return;
        }

        float shift = getResources().getDisplayMetrics().widthPixels * 0.18f;
        heroPanel.setAlpha(0f);
        heroPanel.setTranslationX(shift);
        heroPanel.animate()
                .alpha(1f)
                .translationX(0f)
                .setDuration(HudMotion.ENTER_PANEL)
                .setInterpolator(HudMotion.curve())
                .start();

        textOdometer.setText(Formatters.kmNumber(0));
        heroPanel.postDelayed(new Runnable() {
            @Override
            public void run() {
                animateOdometer(km);
            }
        }, 160L);

        segmentBar.postDelayed(new Runnable() {
            @Override
            public void run() {
                segmentBar.animateTo(health);
            }
        }, 240L);

        statusTag.postDelayed(new Runnable() {
            @Override
            public void run() {
                statusTag.pulseOnce();
            }
        }, 560L);
    }

    private void animateOdometer(int km) {
        if (odometerAnimator != null) {
            odometerAnimator.cancel();
        }
        odometerAnimator = ValueAnimator.ofInt(0, km);
        odometerAnimator.setDuration(HudMotion.ENTER_ODOMETER);
        odometerAnimator.setInterpolator(HudMotion.curve());
        odometerAnimator.addUpdateListener(animation ->
                textOdometer.setText(Formatters.kmNumber((int) animation.getAnimatedValue())));
        odometerAnimator.start();
    }
}
