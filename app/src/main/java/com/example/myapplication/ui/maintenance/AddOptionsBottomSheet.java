package com.example.myapplication.ui.maintenance;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.myapplication.R;
import com.example.myapplication.ui.importinvoice.ImportInvoiceActivity;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

/** Menu do botão flutuante "+". */
public class AddOptionsBottomSheet extends BottomSheetDialogFragment {

    public interface Listener {
        void onManualSelected();

        /** Registro de abastecimento. */
        void onRefuelSelected();

        /** action: null (tela de importação), PHOTO, PDF ou XML. */
        void onImportSelected(String action);
    }

    private Listener listener;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof Listener) {
            listener = (Listener) context;
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.sheet_add_options, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        view.findViewById(R.id.optionManual).setOnClickListener(v -> {
            if (listener != null) {
                listener.onManualSelected();
            }
            dismiss();
        });
        view.findViewById(R.id.optionRefuel).setOnClickListener(v -> {
            if (listener != null) {
                listener.onRefuelSelected();
            }
            dismiss();
        });
        view.findViewById(R.id.optionImport).setOnClickListener(v -> select(null));
        view.findViewById(R.id.optionPhoto).setOnClickListener(v ->
                select(ImportInvoiceActivity.ACTION_PHOTO));
        view.findViewById(R.id.optionPdf).setOnClickListener(v ->
                select(ImportInvoiceActivity.ACTION_PDF));
        view.findViewById(R.id.optionXml).setOnClickListener(v ->
                select(ImportInvoiceActivity.ACTION_XML));
    }

    private void select(String action) {
        if (listener != null) {
            listener.onImportSelected(action);
        }
        dismiss();
    }
}
