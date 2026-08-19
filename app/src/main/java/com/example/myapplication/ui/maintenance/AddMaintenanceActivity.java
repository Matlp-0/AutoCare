package com.example.myapplication.ui.maintenance;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import com.example.myapplication.R;
import com.example.myapplication.data.local.entity.Maintenance;
import com.example.myapplication.data.local.entity.Vehicle;
import com.example.myapplication.domain.model.MaintenanceType;
import com.example.myapplication.util.DateUtils;
import com.example.myapplication.util.Formatters;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Inserção manual e edição de manutenção. */
public class AddMaintenanceActivity extends AppCompatActivity {

    /** Quando informado, a tela edita a manutenção existente em vez de criar uma nova. */
    public static final String EXTRA_MAINTENANCE_ID = "maintenance_id";

    private MaintenanceFormViewModel viewModel;

    private TextInputLayout layoutDate;
    private TextInputLayout layoutKm;
    private TextInputLayout layoutType;
    private TextInputEditText inputDate;
    private TextInputEditText inputKm;
    private TextInputEditText inputDescription;
    private TextInputEditText inputValue;
    private TextInputEditText inputWorkshop;
    private TextInputEditText inputNotes;
    private MaterialAutoCompleteTextView inputType;

    private long selectedDate = System.currentTimeMillis();
    private Vehicle vehicle;
    private long maintenanceId;
    private Maintenance editing;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_maintenance);

        layoutDate = findViewById(R.id.layoutDate);
        layoutKm = findViewById(R.id.layoutKm);
        layoutType = findViewById(R.id.layoutType);
        inputDate = findViewById(R.id.inputDate);
        inputKm = findViewById(R.id.inputKm);
        inputDescription = findViewById(R.id.inputDescription);
        inputValue = findViewById(R.id.inputValue);
        inputWorkshop = findViewById(R.id.inputWorkshop);
        inputNotes = findViewById(R.id.inputNotes);
        inputType = findViewById(R.id.inputType);

        maintenanceId = getIntent().getLongExtra(EXTRA_MAINTENANCE_ID, 0L);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(view -> finish());
        if (maintenanceId > 0) {
            toolbar.setTitle(R.string.edit_maintenance_title);
        }

        List<String> labels = MaintenanceType.allLabels();
        inputType.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, labels));
        inputType.setText(MaintenanceType.OIL_CHANGE.label(), false);

        inputDate.setText(DateUtils.formatShort(selectedDate));
        inputDate.setOnClickListener(view -> showDatePicker());

        findViewById(R.id.buttonSave).setOnClickListener(view -> submit());

        viewModel = new ViewModelProvider(this).get(MaintenanceFormViewModel.class);
        viewModel.vehicle().observe(this, new Observer<Vehicle>() {
            @Override
            public void onChanged(Vehicle value) {
                vehicle = value;
                if (value != null && maintenanceId <= 0 && TextUtils.isEmpty(text(inputKm))) {
                    inputKm.setText(String.valueOf(value.currentKm));
                }
            }
        });
        viewModel.editing().observe(this, new Observer<Maintenance>() {
            @Override
            public void onChanged(Maintenance maintenance) {
                if (maintenance != null) {
                    editing = maintenance;
                    fill(maintenance);
                }
            }
        });
        viewModel.saved().observe(this, new Observer<Boolean>() {
            @Override
            public void onChanged(Boolean success) {
                if (Boolean.TRUE.equals(success)) {
                    Toast.makeText(AddMaintenanceActivity.this,
                            maintenanceId > 0 ? R.string.maintenance_updated
                                    : R.string.maintenance_saved,
                            Toast.LENGTH_SHORT).show();
                    finish();
                }
            }
        });
        viewModel.load(maintenanceId);
    }

    private void fill(Maintenance maintenance) {
        selectedDate = maintenance.date;
        inputDate.setText(DateUtils.formatShort(selectedDate));
        inputKm.setText(String.valueOf(maintenance.odometerKm));
        inputType.setText(MaintenanceType.fromName(maintenance.category).label(), false);
        inputDescription.setText(maintenance.description);
        inputValue.setText(String.format(Locale.US, "%.2f", maintenance.totalCost));
        inputWorkshop.setText(maintenance.workshop);
        inputNotes.setText(maintenance.notes);
    }

    private void showDatePicker() {
        MaterialDatePicker<Long> picker = MaterialDatePicker.Builder.datePicker()
                .setSelection(selectedDate)
                .build();
        picker.addOnPositiveButtonClickListener(selection -> {
            selectedDate = selection;
            inputDate.setText(DateUtils.formatShort(selectedDate));
        });
        picker.show(getSupportFragmentManager(), "date_picker");
    }

    private void submit() {
        layoutDate.setError(null);
        layoutKm.setError(null);
        layoutType.setError(null);

        if (vehicle == null) {
            Toast.makeText(this, R.string.schedule_empty, Toast.LENGTH_SHORT).show();
            return;
        }

        boolean valid = true;
        String dateText = text(inputDate);
        if (TextUtils.isEmpty(dateText)) {
            layoutDate.setError(getString(R.string.error_required));
            valid = false;
        } else {
            try {
                selectedDate = DateUtils.parseShort(dateText);
            } catch (ParseException error) {
                layoutDate.setError(getString(R.string.error_required));
                valid = false;
            }
        }

        int km = 0;
        String kmText = text(inputKm);
        if (TextUtils.isEmpty(kmText)) {
            layoutKm.setError(getString(R.string.error_required));
            valid = false;
        } else {
            try {
                km = Integer.parseInt(kmText);
            } catch (NumberFormatException error) {
                km = -1;
            }
            if (km < 0) {
                layoutKm.setError(getString(R.string.error_km_invalid));
                valid = false;
            }
        }

        MaintenanceType type = MaintenanceType.fromLabel(text(inputType));
        if (TextUtils.isEmpty(text(inputType))) {
            layoutType.setError(getString(R.string.error_required));
            valid = false;
        }

        if (!valid) {
            return;
        }

        boolean isEdit = maintenanceId > 0 && editing != null;
        Maintenance maintenance = isEdit ? editing : new Maintenance();
        maintenance.vehicleId = isEdit ? editing.vehicleId : vehicle.id;
        maintenance.date = selectedDate;
        maintenance.odometerKm = km;
        maintenance.category = type.name();
        maintenance.description = TextUtils.isEmpty(text(inputDescription))
                ? type.label() : text(inputDescription);
        maintenance.totalCost = Formatters.parseMoney(text(inputValue));
        maintenance.workshop = text(inputWorkshop);
        maintenance.notes = text(inputNotes);

        if (isEdit) {
            // Peças e documento importados continuam vinculados à manutenção.
            viewModel.update(maintenance, null);
        } else {
            maintenance.source = Maintenance.SOURCE_MANUAL;
            viewModel.save(maintenance, new ArrayList<>(), null);
        }
    }

    private String text(TextView view) {
        return view.getText() == null ? "" : view.getText().toString().trim();
    }
}
