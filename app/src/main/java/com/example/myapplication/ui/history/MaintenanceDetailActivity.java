package com.example.myapplication.ui.history;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import com.example.myapplication.AppContainer;
import com.example.myapplication.AutoCareApp;
import com.example.myapplication.R;
import com.example.myapplication.data.local.entity.Document;
import com.example.myapplication.data.local.entity.MaintenanceItem;
import com.example.myapplication.data.local.relation.MaintenanceWithItems;
import com.example.myapplication.domain.model.MaintenanceType;
import com.example.myapplication.ui.maintenance.AddMaintenanceActivity;
import com.example.myapplication.ui.maintenance.MaintenanceFormViewModel;
import com.example.myapplication.util.DateUtils;
import com.example.myapplication.util.Formatters;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class MaintenanceDetailActivity extends AppCompatActivity {

    public static final String EXTRA_MAINTENANCE_ID = "maintenance_id";

    private MaintenanceFormViewModel viewModel;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maintenance_detail);

        final long id = getIntent().getLongExtra(EXTRA_MAINTENANCE_ID, 0L);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });
        toolbar.inflateMenu(R.menu.menu_maintenance_detail);
        toolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                if (item.getItemId() == R.id.action_edit) {
                    Intent intent = new Intent(MaintenanceDetailActivity.this,
                            AddMaintenanceActivity.class);
                    intent.putExtra(AddMaintenanceActivity.EXTRA_MAINTENANCE_ID, id);
                    startActivity(intent);
                    return true;
                }
                if (item.getItemId() == R.id.action_delete) {
                    confirmDelete(id);
                    return true;
                }
                return false;
            }
        });

        viewModel = new ViewModelProvider(this).get(MaintenanceFormViewModel.class);
        viewModel.deleted().observe(this, new Observer<Boolean>() {
            @Override
            public void onChanged(Boolean success) {
                if (Boolean.TRUE.equals(success)) {
                    Toast.makeText(MaintenanceDetailActivity.this, R.string.maintenance_deleted,
                            Toast.LENGTH_SHORT).show();
                    finish();
                }
            }
        });

        final TextView textTitle = findViewById(R.id.textTitle);
        final TextView textSubtitle = findViewById(R.id.textSubtitle);
        final TextView textItems = findViewById(R.id.textItems);
        final TextView textDetails = findViewById(R.id.textDetails);
        final TextView textDocument = findViewById(R.id.textDocument);

        AppContainer container = AutoCareApp.container(this);
        container.maintenanceRepository.observeById(id).observe(this,
                new Observer<MaintenanceWithItems>() {
                    @Override
                    public void onChanged(MaintenanceWithItems entry) {
                        if (entry == null || entry.maintenance == null) {
                            return;
                        }
                        MaintenanceType type = MaintenanceType.fromName(entry.maintenance.category);
                        String title = entry.maintenance.description;
                        if (title == null || title.trim().isEmpty()) {
                            title = type.label();
                        }
                        textTitle.setText(title);
                        textSubtitle.setText(DateUtils.formatShort(entry.maintenance.date) + " • "
                                + Formatters.km(entry.maintenance.odometerKm) + " • "
                                + type.label());

                        StringBuilder items = new StringBuilder();
                        for (MaintenanceItem item : entry.items) {
                            if (items.length() > 0) {
                                items.append('\n');
                            }
                            items.append("• ").append(item.name);
                            if (item.totalPrice > 0) {
                                items.append(" — ").append(Formatters.money(item.totalPrice));
                            }
                        }
                        textItems.setText(items.length() == 0 ? "—" : items.toString());

                        StringBuilder details = new StringBuilder();
                        details.append("Valor: ").append(Formatters.money(entry.maintenance.totalCost));
                        if (entry.maintenance.workshop != null && !entry.maintenance.workshop.isEmpty()) {
                            details.append("\nOficina: ").append(entry.maintenance.workshop);
                        }
                        if (entry.maintenance.notes != null && !entry.maintenance.notes.isEmpty()) {
                            details.append("\nObservações: ").append(entry.maintenance.notes);
                        }
                        textDetails.setText(details.toString());

                        if (entry.documents.isEmpty()) {
                            textDocument.setText(R.string.detail_no_document);
                        } else {
                            Document document = entry.documents.get(0);
                            StringBuilder builder = new StringBuilder(getString(R.string.detail_document));
                            builder.append(": ").append(document.type);
                            if (document.invoiceNumber != null) {
                                builder.append(" • NF ").append(document.invoiceNumber);
                            }
                            if (document.companyName != null) {
                                builder.append(" • ").append(document.companyName);
                            }
                            textDocument.setText(builder.toString());
                        }
                    }
                });
    }

    /** Exclusão é irreversível: sempre confirma antes. */
    private void confirmDelete(final long maintenanceId) {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.delete_confirm_title)
                .setMessage(R.string.delete_confirm_message)
                .setPositiveButton(R.string.action_delete, (dialog, which) ->
                        viewModel.delete(maintenanceId))
                .setNegativeButton(R.string.carbon_cancel, null)
                .show();
    }
}
