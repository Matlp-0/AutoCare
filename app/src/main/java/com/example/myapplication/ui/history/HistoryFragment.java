package com.example.myapplication.ui.history;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
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

import com.example.myapplication.R;
import com.example.myapplication.data.local.entity.Vehicle;
import com.example.myapplication.data.local.relation.MaintenanceWithItems;
import com.example.myapplication.domain.export.MaintenanceHistoryPdf;
import com.example.myapplication.domain.model.DashboardState;
import com.example.myapplication.domain.model.UpcomingMaintenance;
import com.example.myapplication.ui.carbon.CarbonFilterTabs;
import com.example.myapplication.ui.carbon.CarbonSystemBars;
import com.example.myapplication.ui.carbon.CarbonTechnicalRow;
import com.example.myapplication.ui.main.MainActivity;
import com.example.myapplication.ui.main.VehicleViewModel;
import com.example.myapplication.ui.maintenance.AddMaintenanceActivity;
import com.example.myapplication.ui.maintenance.MaintenanceFormViewModel;
import com.example.myapplication.util.AppExecutors;
import com.example.myapplication.util.Formatters;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Manutenções no Carbon UI: central técnica com pendentes (motor de manutenção) e
 * concluídas (histórico).
 *
 * <p>As duas listas já vinham do {@code VehicleViewModel}; nada de novo é calculado
 * aqui e os fluxos de abrir, editar e excluir seguem idênticos.
 */
public class HistoryFragment extends Fragment {

    private static final int TAB_ALL = 0;
    private static final int TAB_PENDING = 1;
    private static final int TAB_DONE = 2;

    private MaintenanceListAdapter adapter;
    private MaintenanceFormViewModel formViewModel;
    private CarbonFilterTabs filterTabs;
    private CarbonTechnicalRow rowSummary;
    private TextView textEmpty;
    private View groupEmpty;
    private TextView buttonExportPdf;
    private Vehicle vehicle;

    private final List<UpcomingMaintenance> pending = new ArrayList<>();
    private final List<MaintenanceWithItems> completed = new ArrayList<>();
    private int selectedTab = TAB_ALL;
    private boolean firstRender = true;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_history, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        textEmpty = view.findViewById(R.id.textEmpty);
        groupEmpty = view.findViewById(R.id.groupEmpty);
        view.findViewById(R.id.buttonEmptyAction).setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View clicked) {
                        startActivity(new Intent(requireContext(),
                                AddMaintenanceActivity.class));
                    }
                });
        buttonExportPdf = view.findViewById(R.id.buttonExportPdf);
        buttonExportPdf.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View clicked) {
                exportPdf();
            }
        });
        rowSummary = view.findViewById(R.id.rowSummary);
        filterTabs = view.findViewById(R.id.filterTabs);

        filterTabs.setTabs(getString(R.string.carbon_tab_all),
                getString(R.string.carbon_tab_pending),
                getString(R.string.carbon_tab_done));
        filterTabs.select(selectedTab, false);
        filterTabs.setOnTabSelected(new CarbonFilterTabs.OnTabSelected() {
            @Override
            public void onTabSelected(int index) {
                selectedTab = index;
                bindList();
            }
        });

        formViewModel = new ViewModelProvider(this).get(MaintenanceFormViewModel.class);
        formViewModel.deleted().observe(getViewLifecycleOwner(), new Observer<Boolean>() {
            @Override
            public void onChanged(Boolean success) {
                if (Boolean.TRUE.equals(success)) {
                    Toast.makeText(requireContext(), R.string.maintenance_deleted,
                            Toast.LENGTH_SHORT).show();
                }
            }
        });

        adapter = new MaintenanceListAdapter(new MaintenanceListAdapter.Listener() {
            @Override
            public void onCompletedClick(MaintenanceWithItems item) {
                Intent intent = new Intent(requireContext(), MaintenanceDetailActivity.class);
                intent.putExtra(MaintenanceDetailActivity.EXTRA_MAINTENANCE_ID,
                        item.maintenance.id);
                startActivity(intent);
            }

            @Override
            public void onCompletedLongClick(MaintenanceWithItems item) {
                showQuickActions(item);
            }

            @Override
            public void onPendingClick(UpcomingMaintenance item) {
                // Item previsto não tem registro para abrir: leva ao cronograma,
                // mesmo destino do "DETALHES" da Home.
                if (getActivity() instanceof MainActivity) {
                    ((MainActivity) getActivity()).openScheduleTab();
                }
            }
        });

        RecyclerView recycler = view.findViewById(R.id.recyclerHistory);
        recycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        recycler.setAdapter(adapter);

        VehicleViewModel viewModel = new ViewModelProvider(requireActivity())
                .get(VehicleViewModel.class);
        viewModel.history().observe(getViewLifecycleOwner(),
                new Observer<List<MaintenanceWithItems>>() {
                    @Override
                    public void onChanged(List<MaintenanceWithItems> items) {
                        completed.clear();
                        if (items != null) {
                            completed.addAll(items);
                        }
                        bindList();
                    }
                });
        viewModel.vehicle().observe(getViewLifecycleOwner(), new Observer<Vehicle>() {
            @Override
            public void onChanged(Vehicle active) {
                vehicle = active;
            }
        });
        viewModel.dashboard().observe(getViewLifecycleOwner(), new Observer<DashboardState>() {
            @Override
            public void onChanged(DashboardState state) {
                pending.clear();
                if (state != null) {
                    pending.addAll(state.schedule.all);
                }
                bindList();
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        CarbonSystemBars.applyOnResume(getActivity(), getView());
    }

    private void bindList() {
        boolean showPending = selectedTab != TAB_DONE;
        boolean showDone = selectedTab != TAB_PENDING;

        List<UpcomingMaintenance> pendingRows = showPending ? pending : null;
        List<MaintenanceWithItems> doneRows = showDone ? completed : null;
        adapter.submit(pendingRows, doneRows, selectedTab == TAB_ALL);

        boolean empty = (pendingRows == null || pendingRows.isEmpty())
                && (doneRows == null || doneRows.isEmpty());
        groupEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
        textEmpty.setText(emptyMessage());

        bindSummary();

        if (firstRender) {
            firstRender = false;
            animateIn();
        }
    }

    private int emptyMessage() {
        if (selectedTab == TAB_PENDING) {
            return R.string.carbon_empty_pending;
        }
        if (selectedTab == TAB_DONE) {
            return R.string.carbon_empty_done;
        }
        return R.string.history_empty;
    }

    private void bindSummary() {
        if (selectedTab == TAB_PENDING) {
            rowSummary.setLabel(getString(R.string.carbon_pending_count));
            rowSummary.setValue(getString(R.string.carbon_count_value, pending.size()));
            rowSummary.setValueColor(R.color.carbon_text_primary);
            return;
        }
        double total = 0d;
        for (MaintenanceWithItems entry : completed) {
            total += entry.maintenance.totalCost;
        }
        rowSummary.setLabel(getString(R.string.carbon_total_spent));
        rowSummary.setValue(Formatters.money(total));
        rowSummary.setValueColor(R.color.carbon_cyan);
    }

    /** Mesma entrada discreta da Home: deslize horizontal curto. */
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

    /**
     * Exporta todo o histórico concluído em PDF na pasta Downloads.
     * A geração roda no diskIO porque desenha o documento e grava o arquivo.
     */
    private void exportPdf() {
        if (completed.isEmpty()) {
            Toast.makeText(requireContext(), R.string.export_pdf_empty, Toast.LENGTH_SHORT).show();
            return;
        }
        final Context context = requireContext().getApplicationContext();
        final Vehicle target = vehicle;
        final List<MaintenanceWithItems> snapshot = new ArrayList<>(completed);

        buttonExportPdf.setEnabled(false);
        Toast.makeText(context, R.string.export_pdf_running, Toast.LENGTH_SHORT).show();

        AppExecutors.get().diskIO().execute(new Runnable() {
            @Override
            public void run() {
                Uri saved = null;
                String failure = null;
                try {
                    saved = MaintenanceHistoryPdf.exportToDownloads(context, target, snapshot);
                } catch (IOException | RuntimeException error) {
                    failure = String.valueOf(error.getMessage());
                }
                final Uri result = saved;
                final String message = failure;
                AppExecutors.get().mainThread(new Runnable() {
                    @Override
                    public void run() {
                        onExportFinished(result, message);
                    }
                });
            }
        });
    }

    private void onExportFinished(final Uri saved, String failure) {
        if (!isAdded()) {
            return;
        }
        buttonExportPdf.setEnabled(true);
        if (saved == null) {
            Toast.makeText(requireContext(), getString(R.string.export_pdf_error, failure),
                    Toast.LENGTH_LONG).show();
            return;
        }
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.export_pdf_title)
                .setMessage(R.string.export_pdf_message)
                .setPositiveButton(R.string.export_pdf_open, (dialog, which) -> openPdf(saved))
                .setNegativeButton(R.string.carbon_cancel, null)
                .show();
    }

    private void openPdf(Uri saved) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(saved, "application/pdf");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException error) {
            Toast.makeText(requireContext(), R.string.export_pdf_no_viewer,
                    Toast.LENGTH_SHORT).show();
        }
    }

    private void showQuickActions(final MaintenanceWithItems entry) {
        String[] options = {getString(R.string.action_edit), getString(R.string.action_delete)};
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(entry.maintenance.description)
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        Intent intent = new Intent(requireContext(), AddMaintenanceActivity.class);
                        intent.putExtra(AddMaintenanceActivity.EXTRA_MAINTENANCE_ID,
                                entry.maintenance.id);
                        startActivity(intent);
                    } else {
                        confirmDelete(entry);
                    }
                })
                .show();
    }

    private void confirmDelete(final MaintenanceWithItems entry) {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.delete_confirm_title)
                .setMessage(R.string.delete_confirm_message)
                .setPositiveButton(R.string.action_delete, (dialog, which) ->
                        formViewModel.delete(entry.maintenance.id))
                .setNegativeButton(R.string.carbon_cancel, null)
                .show();
    }
}
