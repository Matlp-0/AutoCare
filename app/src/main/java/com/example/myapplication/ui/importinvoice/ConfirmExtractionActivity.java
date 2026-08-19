package com.example.myapplication.ui.importinvoice;

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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.data.local.entity.Document;
import com.example.myapplication.data.local.entity.Maintenance;
import com.example.myapplication.data.local.entity.MaintenanceItem;
import com.example.myapplication.data.local.entity.Vehicle;
import com.example.myapplication.domain.document.ExtractedInvoice;
import com.example.myapplication.domain.document.ExtractedProduct;
import com.example.myapplication.domain.model.MaintenanceType;
import com.example.myapplication.ui.maintenance.MaintenanceFormViewModel;
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

/**
 * Revisão do que foi extraído do documento. Nada é gravado sem o usuário confirmar.
 */
public class ConfirmExtractionActivity extends AppCompatActivity {

    public static final String EXTRA_INVOICE = "invoice";

    private final ExtractedProductAdapter adapter = new ExtractedProductAdapter();

    private MaintenanceFormViewModel viewModel;
    private ExtractedInvoice invoice;
    private Vehicle vehicle;
    private long selectedDate = System.currentTimeMillis();

    private TextInputLayout layoutKm;
    private TextInputEditText inputDate;
    private TextInputEditText inputWorkshop;
    private TextInputEditText inputKm;
    private TextInputEditText inputValue;
    private MaterialAutoCompleteTextView inputType;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_confirm_extraction);

        invoice = (ExtractedInvoice) getIntent().getSerializableExtra(EXTRA_INVOICE);
        if (invoice == null) {
            finish();
            return;
        }

        layoutKm = findViewById(R.id.layoutKm);
        inputDate = findViewById(R.id.inputDate);
        inputWorkshop = findViewById(R.id.inputWorkshop);
        inputKm = findViewById(R.id.inputKm);
        inputValue = findViewById(R.id.inputValue);
        inputType = findViewById(R.id.inputType);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(view -> finish());

        RecyclerView recycler = findViewById(R.id.recyclerProducts);
        recycler.setLayoutManager(new LinearLayoutManager(this));
        recycler.setAdapter(adapter);
        adapter.submit(invoice.products);

        inputType.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1,
                MaintenanceType.allLabels()));
        inputType.setText(invoice.suggestedType.label(), false);

        selectedDate = invoice.issueDate > 0 ? invoice.issueDate : System.currentTimeMillis();
        inputDate.setText(DateUtils.formatShort(selectedDate));
        inputDate.setOnClickListener(view -> showDatePicker());

        if (!TextUtils.isEmpty(invoice.companyName)) {
            inputWorkshop.setText(invoice.companyName);
        }
        if (invoice.totalValue > 0) {
            inputValue.setText(String.format(java.util.Locale.US, "%.2f", invoice.totalValue));
        }

        TextView textInvoiceInfo = findViewById(R.id.textInvoiceInfo);
        textInvoiceInfo.setText(invoiceSummary());

        findViewById(R.id.buttonConfirm).setOnClickListener(view -> confirm());

        viewModel = new ViewModelProvider(this).get(MaintenanceFormViewModel.class);
        viewModel.vehicle().observe(this, new Observer<Vehicle>() {
            @Override
            public void onChanged(Vehicle value) {
                vehicle = value;
                if (value != null && TextUtils.isEmpty(text(inputKm))) {
                    inputKm.setText(String.valueOf(value.currentKm));
                }
            }
        });
        viewModel.saved().observe(this, new Observer<Boolean>() {
            @Override
            public void onChanged(Boolean success) {
                if (Boolean.TRUE.equals(success)) {
                    Toast.makeText(ConfirmExtractionActivity.this, R.string.maintenance_saved,
                            Toast.LENGTH_SHORT).show();
                    finish();
                }
            }
        });
    }

    private String invoiceSummary() {
        StringBuilder builder = new StringBuilder();
        builder.append("Documento: ").append(invoice.documentType);
        if (!TextUtils.isEmpty(invoice.invoiceNumber)) {
            builder.append(" • NF ").append(invoice.invoiceNumber);
        }
        if (!TextUtils.isEmpty(invoice.cnpj)) {
            builder.append(" • CNPJ ").append(invoice.cnpj);
        }
        if (invoice.totalValue > 0) {
            builder.append(" • ").append(Formatters.money(invoice.totalValue));
        }
        if (invoice.products.isEmpty()) {
            builder.append("\nNenhum produto identificado automaticamente — complete os dados acima.");
        }
        return builder.toString();
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

    private void confirm() {
        if (vehicle == null) {
            Toast.makeText(this, R.string.schedule_empty, Toast.LENGTH_SHORT).show();
            return;
        }
        layoutKm.setError(null);

        try {
            selectedDate = DateUtils.parseShort(text(inputDate));
        } catch (ParseException ignored) {
            // mantém a data já selecionada
        }

        int km;
        try {
            km = Integer.parseInt(text(inputKm));
        } catch (NumberFormatException error) {
            km = -1;
        }
        if (km < 0) {
            layoutKm.setError(getString(R.string.error_km_invalid));
            return;
        }

        MaintenanceType type = MaintenanceType.fromLabel(text(inputType));
        List<ExtractedProduct> selected = adapter.selected();

        Maintenance maintenance = new Maintenance();
        maintenance.vehicleId = vehicle.id;
        maintenance.date = selectedDate;
        maintenance.odometerKm = km;
        maintenance.category = type.name();
        maintenance.description = TextUtils.isEmpty(invoice.suggestedDescription)
                ? type.label() : invoice.suggestedDescription;
        maintenance.totalCost = Formatters.parseMoney(text(inputValue));
        maintenance.workshop = text(inputWorkshop);
        maintenance.notes = "Importado de " + invoice.documentType;
        maintenance.source = invoice.documentType;

        List<MaintenanceItem> items = new ArrayList<>();
        for (ExtractedProduct product : selected) {
            MaintenanceItem item = new MaintenanceItem();
            item.name = product.name;
            item.type = product.suggestedType.name();
            item.quantity = product.quantity;
            item.unitPrice = product.unitPrice;
            item.totalPrice = product.totalPrice;
            items.add(item);
        }

        Document document = new Document();
        document.type = invoice.documentType;
        document.uri = invoice.uri;
        document.invoiceNumber = invoice.invoiceNumber;
        document.cnpj = invoice.cnpj;
        document.companyName = invoice.companyName;
        document.issueDate = invoice.issueDate;
        document.totalValue = invoice.totalValue;
        document.rawText = invoice.rawText;

        viewModel.save(maintenance, items, document);
    }

    private String text(TextView view) {
        return view.getText() == null ? "" : view.getText().toString().trim();
    }
}
