package com.example.myapplication.ui.schedule;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.AutoCareApp;
import com.example.myapplication.R;
import com.example.myapplication.domain.manual.ManualInfo;
import com.example.myapplication.util.Callback;
import com.example.myapplication.ui.carbon.CarbonSystemBars;
import com.example.myapplication.domain.model.DashboardState;
import com.example.myapplication.domain.model.RevisionMilestone;
import com.example.myapplication.ui.main.VehicleViewModel;
import com.example.myapplication.util.Formatters;

public class ScheduleFragment extends Fragment {

    private TimelineAdapter adapter;
    private VehicleViewModel viewModel;

    private com.example.myapplication.data.local.entity.Vehicle currentVehicle;
    private boolean firstRender = true;
    private boolean searching;

    private TextView textVehicleKm;
    private TextView textEmpty;
    private View groupEmpty;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_schedule, container, false);
    }

    @Override
    public void onResume() {
        super.onResume();
        CarbonSystemBars.applyOnResume(getActivity(), getView());
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        textVehicleKm = view.findViewById(R.id.textVehicleKm);
        textEmpty = view.findViewById(R.id.textEmpty);
        groupEmpty = view.findViewById(R.id.groupEmpty);

        final TextView buttonFindPlan = view.findViewById(R.id.buttonFindPlan);
        buttonFindPlan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View clicked) {
                if (currentVehicle == null) {
                    return;
                }
                buttonFindPlan.setEnabled(false);
                searching = true;
                textEmpty.setText(R.string.identification_searching);
                // Mesma busca do botão em "Meu carro": nenhuma regra nova aqui.
                AutoCareApp.container(requireContext()).planRepository
                        .refreshFromWeb(currentVehicle, new Callback<ManualInfo>() {
                            @Override
                            public void onResult(ManualInfo info) {
                                buttonFindPlan.setEnabled(true);
                                searching = false;
                                if (info == null || info.plan.isEmpty()) {
                                    textEmpty.setText(R.string.plan_not_found);
                                    return;
                                }
                                viewModel.refreshPlan();
                            }
                        });
            }
        });

        viewModel = new ViewModelProvider(requireActivity()).get(VehicleViewModel.class);

        adapter = new TimelineAdapter(new TimelineAdapter.OnRevisionChecked() {
            @Override
            public void onChecked(RevisionMilestone milestone, boolean done) {
                viewModel.setRevisionDone(milestone.km, done);
            }
        });

        RecyclerView recycler = view.findViewById(R.id.recyclerTimeline);
        recycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        recycler.setAdapter(adapter);
        viewModel.dashboard().observe(getViewLifecycleOwner(), new Observer<DashboardState>() {
            @Override
            public void onChanged(DashboardState state) {
                if (state == null || state.vehicle == null) {
                    groupEmpty.setVisibility(View.VISIBLE);
                    return;
                }
                textVehicleKm.setText(state.vehicle.displayName() + " • "
                        + Formatters.km(state.vehicle.currentKm));
                adapter.submit(state.timeline, state.vehicle.currentKm);
                currentVehicle = state.vehicle;
                boolean empty = state.timeline.isEmpty();
                groupEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
                if (empty && !searching) {
                    textEmpty.setText(R.string.schedule_empty);
                }

                if (firstRender) {
                    firstRender = false;
                    animateIn();
                }
            }
        });
    }

    /** Mesma entrada discreta da Home e de Manutenções. */
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
