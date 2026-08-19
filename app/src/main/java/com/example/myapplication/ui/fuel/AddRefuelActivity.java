package com.example.myapplication.ui.fuel;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import com.example.myapplication.R;
import com.example.myapplication.data.local.entity.Refuel;
import com.example.myapplication.data.local.entity.Vehicle;
import com.example.myapplication.domain.model.FuelType;
import com.example.myapplication.util.DateUtils;
import com.example.myapplication.util.Formatters;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.Locale;

/** Registro e edição de abastecimento. */
public class AddRefuelActivity extends AppCompatActivity {

    /** Quando informado, edita o abastecimento existente. */
    public static final String EXTRA_REFUEL_ID = "refuel_id";

    private RefuelFormViewModel viewModel;

    private TextInputLayout layoutKm;
    private TextInputLayout layoutLiters;
    private TextInputEditText inputDate;
    private TextInputEditText inputKm;
    private TextInputEditText inputLiters;
    private TextInputEditText inputPrice;
    private TextInputEditText inputTotal;
    private TextInputEditText inputStation;
    private TextInputEditText inputNotes;
    private MaterialAutoCompleteTextView inputFuelType;
    private MaterialCheckBox checkFullTank;
    private MaterialCheckBox checkMissedPrevious;

    private long selectedDate = System.currentTimeMillis();
    private Vehicle vehicle;
    private long refuelId;
    private Refuel editing;
    /** Evita que o preenchimento automático de um campo dispare o do outro. */
    private boolean syncingTotals;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_refuel);

        layoutKm = findViewById(R.id.layoutKm);
        layoutLiters = findViewById(R.id.layoutLiters);
        inputDate = findViewById(R.id.inputDate);
        inputKm = findViewById(R.id.inputKm);
        inputLiters = findViewById(R.id.inputLiters);
        inputPrice = findViewById(R.id.inputPrice);
        inputTotal = findViewById(R.id.inputTotal);
        inputStation = findViewById(R.id.inputStation);
        inputNotes = findViewById(R.id.inputNotes);
        inputFuelType = findViewById(R.id.inputFuelType);
        checkFullTank = findViewById(R.id.checkFullTank);
        checkMissedPrevious = findViewById(R.id.checkMissedPrevious);

        refuelId = getIntent().getLongExtra(EXTRA_REFUEL_ID, 0L);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(view -> finish());
        if (refuelId > 0L) {
            toolbar.setTitle(R.string.fuel_edit);
        }

        inputFuelType.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1,
                FuelType.allLabels()));
        inputFuelType.setText(FuelType.GASOLINE.label(), false);

        inputDate.setText(DateUtils.formatShort(selectedDate));
        inputDate.setOnClickListener(view -> showDatePicker());

        // Litros × preço ↔ total: preenche o que faltar sem sobrescrever o digitado.
        inputLiters.addTextChangedListener(new Recalculate(true));
        inputPrice.addTextChangedListener(new Recalculate(true));
        inputTotal.addTextChangedListener(new Recalculate(false));

        findViewById(R.id.buttonSave).setOnClickListener(view -> submit());

        viewModel = new ViewModelProvider(this).get(RefuelFormViewModel.class);
        viewModel.vehicle().observe(this, new Observer<Vehicle>() {
            @Override
            public void onChanged(Vehicle value) {
                vehicle = value;
                if (value != null && refuelId <= 0L && TextUtils.isEmpty(text(inputKm))) {
                    inputKm.setText(String.valueOf(value.currentKm));
                }
            }
        });
        viewModel.editing().observe(this, new Observer<Refuel>() {
            @Override
            public void onChanged(Refuel refuel) {
                if (refuel != null) {
                    editing = refuel;
                    fill(refuel);
                }
            }
        });
        viewModel.saved().observe(this, new Observer<Boolean>() {
            @Override
            public void onChanged(Boolean success) {
                if (Boolean.TRUE.equals(success)) {
                    Toast.makeText(AddRefuelActivity.this, R.string.fuel_saved,
                            Toast.LENGTH_SHORT).show();
                    finish();
                }
            }
        });
        viewModel.load(refuelId);
    }

    private void fill(Refuel refuel) {
        selectedDate = refuel.date;
        syncingTotals = true;
        inputDate.setText(DateUtils.formatShort(selectedDate));
        inputKm.setText(String.valueOf(refuel.odometerKm));
        inputFuelType.setText(FuelType.fromName(refuel.fuelType).label(), false);
        inputLiters.setText(String.format(Locale.US, "%.2f", refuel.liters));
        inputPrice.setText(String.format(Locale.US, "%.3f", refuel.pricePerLiter));
        inputTotal.setText(String.format(Locale.US, "%.2f", refuel.totalCost));
        inputStation.setText(refuel.station);
        inputNotes.setText(refuel.notes);
        checkFullTank.setChecked(refuel.fullTank);
        checkMissedPrevious.setChecked(refuel.missedPrevious);
        syncingTotals = false;
    }

    private void showDatePicker() {
        MaterialDatePicker<Long> picker = MaterialDatePicker.Builder.datePicker()
                .setSelection(selectedDate)
                .build();
        picker.addOnPositiveButtonClickListener(selection -> {
            selectedDate = selection;
            inputDate.setText(DateUtils.formatShort(selectedDate));
        });
        picker.show(getSupportFragmentManager(), "refuel_date");
    }

    private void submit() {
        layoutKm.setError(null);
        layoutLiters.setError(null);

        int km = parseKm(text(inputKm));
        if (km <= 0) {
            layoutKm.setError(getString(R.string.error_km_invalid));
            return;
        }
        double liters = Formatters.parseDecimal(text(inputLiters));
        if (liters <= 0d) {
            layoutLiters.setError(getString(R.string.error_liters_invalid));
            return;
        }
        double price = Formatters.parseDecimal(text(inputPrice));
        double total = Formatters.parseDecimal(text(inputTotal));
        if (total <= 0d && price > 0d) {
            total = liters * price;
        }
        if (price <= 0d && total > 0d) {
            price = total / liters;
        }
        if (vehicle == null) {
            Toast.makeText(this, R.string.error_no_vehicle, Toast.LENGTH_SHORT).show();
            return;
        }

        Refuel refuel = editing != null ? editing : new Refuel();
        refuel.id = refuelId;
        refuel.vehicleId = vehicle.id;
        refuel.date = selectedDate;
        refuel.odometerKm = km;
        refuel.liters = liters;
        refuel.pricePerLiter = price;
        refuel.totalCost = total;
        refuel.fuelType = FuelType.fromLabel(inputFuelType.getText().toString()).name();
        refuel.station = text(inputStation);
        refuel.notes = text(inputNotes);
        refuel.fullTank = checkFullTank.isChecked();
        refuel.missedPrevious = checkMissedPrevious.isChecked();

        viewModel.save(refuel);
    }

    private int parseKm(String value) {
        try {
            return Integer.parseInt(value.replaceAll("[^0-9]", ""));
        } catch (NumberFormatException error) {
            return 0;
        }
    }

    private String text(TextInputEditText input) {
        return input.getText() == null ? "" : input.getText().toString().trim();
    }

    /**
     * Mantém litros, preço e total coerentes: alterar litros ou preço recalcula o
     * total; alterar o total recalcula o preço por litro.
     */
    private class Recalculate implements TextWatcher {

        private final boolean fromLitersOrPrice;

        Recalculate(boolean fromLitersOrPrice) {
            this.fromLitersOrPrice = fromLitersOrPrice;
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
        }

        @Override
        public void afterTextChanged(Editable editable) {
            if (syncingTotals) {
                return;
            }
            double liters = Formatters.parseDecimal(text(inputLiters));
            syncingTotals = true;
            if (fromLitersOrPrice) {
                double price = Formatters.parseDecimal(text(inputPrice));
                if (liters > 0d && price > 0d) {
                    inputTotal.setText(String.format(Locale.US, "%.2f", liters * price));
                }
            } else {
                double total = Formatters.parseDecimal(text(inputTotal));
                if (liters > 0d && total > 0d) {
                    inputPrice.setText(String.format(Locale.US, "%.3f", total / liters));
                }
            }
            syncingTotals = false;
        }
    }
}
