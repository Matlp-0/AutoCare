package com.example.myapplication.ui.carbon;

import android.content.Context;
import android.text.InputType;
import android.widget.EditText;
import android.widget.Toast;

import com.example.myapplication.R;
import com.example.myapplication.ui.main.VehicleViewModel;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

/**
 * Diálogo de atualização da quilometragem.
 *
 * <p>Atualizar a km é a ação mais frequente do usuário, então ela aparece tanto na
 * Home quanto em Meu carro. O diálogo é o mesmo e chama o mesmo método do
 * ViewModel — não existe segunda regra de negócio.
 */
public final class KmUpdateDialog {

    private KmUpdateDialog() {
    }

    public static void show(final Context context, final VehicleViewModel viewModel,
                            int currentKm) {
        if (context == null || viewModel == null) {
            return;
        }
        final EditText input = new EditText(context);
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        input.setText(String.valueOf(currentKm));
        input.setSelectAllOnFocus(true);

        new MaterialAlertDialogBuilder(context)
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
                        Toast.makeText(context, R.string.error_km_invalid,
                                Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton(R.string.carbon_cancel, null)
                .show();
    }
}
