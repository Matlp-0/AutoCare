package com.example.myapplication.ui.onboarding;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import com.example.myapplication.R;
import com.example.myapplication.data.local.entity.Vehicle;
import com.example.myapplication.util.AppExecutors;
import com.example.myapplication.util.ImageStore;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.io.IOException;
import java.util.Calendar;

/** Cadastro e edição do veículo. Toda validação acontece aqui, na borda da UI. */
public class VehicleFormActivity extends AppCompatActivity {

    public static final String EXTRA_VEHICLE_ID = "vehicle_id";

    private static final String[] FUELS = {"Gasolina", "Etanol", "Flex", "Diesel", "GNV", "Híbrido", "Elétrico"};
    private static final String[] TRANSMISSIONS = {"Manual", "Automático", "CVT", "Automatizado"};

    private VehicleFormViewModel viewModel;

    private TextInputLayout layoutBrand;
    private TextInputLayout layoutModel;
    private TextInputLayout layoutYear;
    private TextInputLayout layoutEngine;
    private TextInputLayout layoutKm;
    private TextInputEditText inputBrand;
    private TextInputEditText inputModel;
    private TextInputEditText inputYear;
    private TextInputEditText inputEngine;
    private TextInputEditText inputKm;
    private MaterialAutoCompleteTextView inputFuel;
    private MaterialAutoCompleteTextView inputTransmission;
    private TextInputEditText inputNickname;
    private ImageView imageVehicle;
    private TextView buttonRemovePhoto;

    private long vehicleId;
    private Vehicle loaded;

    private ActivityResultLauncher<PickVisualMediaRequest> photoPicker;
    /** Foto escolhida nesta edição (já copiada para o app). */
    private String photoPath;
    /** Foto que estava gravada, para apagar o arquivo antigo depois de salvar. */
    private String originalPhotoPath;
    private boolean saved;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_vehicle_form);

        vehicleId = getIntent().getLongExtra(EXTRA_VEHICLE_ID, 0L);
        viewModel = new ViewModelProvider(this).get(VehicleFormViewModel.class);

        bindViews();
        setupDropdowns();
        setupPhoto();

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        TextView buttonContinue = findViewById(R.id.buttonContinue);
        if (vehicleId > 0) {
            toolbar.setTitle(R.string.car_edit);
            buttonContinue.setText(R.string.action_save);
        }
        buttonContinue.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                submit();
            }
        });

        viewModel.vehicle().observe(this, new Observer<Vehicle>() {
            @Override
            public void onChanged(Vehicle vehicle) {
                if (vehicle != null) {
                    loaded = vehicle;
                    fill(vehicle);
                }
            }
        });
        viewModel.savedId().observe(this, new Observer<Long>() {
            @Override
            public void onChanged(Long id) {
                if (id == null || id <= 0) {
                    return;
                }
                saved = true;
                if (originalPhotoPath != null && !originalPhotoPath.equals(photoPath)) {
                    ImageStore.delete(originalPhotoPath);
                }
                if (vehicleId > 0) {
                    finish();
                } else {
                    Intent intent = new Intent(VehicleFormActivity.this,
                            VehicleIdentificationActivity.class);
                    intent.putExtra(VehicleIdentificationActivity.EXTRA_VEHICLE_ID, id);
                    startActivity(intent);
                    finish();
                }
            }
        });
        viewModel.load(vehicleId);
    }

    private void bindViews() {
        layoutBrand = findViewById(R.id.layoutBrand);
        layoutModel = findViewById(R.id.layoutModel);
        layoutYear = findViewById(R.id.layoutYear);
        layoutEngine = findViewById(R.id.layoutEngine);
        layoutKm = findViewById(R.id.layoutKm);
        inputBrand = findViewById(R.id.inputBrand);
        inputModel = findViewById(R.id.inputModel);
        inputYear = findViewById(R.id.inputYear);
        inputEngine = findViewById(R.id.inputEngine);
        inputKm = findViewById(R.id.inputKm);
        inputFuel = findViewById(R.id.inputFuel);
        inputTransmission = findViewById(R.id.inputTransmission);
        inputNickname = findViewById(R.id.inputNickname);
        imageVehicle = findViewById(R.id.imageVehicle);
        buttonRemovePhoto = findViewById(R.id.buttonRemovePhoto);
    }

    /**
     * Foto pelo seletor do sistema. A imagem é copiada para dentro do app porque
     * a Uri devolvida vale só para esta sessão.
     */
    private void setupPhoto() {
        photoPicker = registerForActivityResult(
                new ActivityResultContracts.PickVisualMedia(), new androidx.activity.result
                        .ActivityResultCallback<Uri>() {
                    @Override
                    public void onActivityResult(Uri uri) {
                        if (uri != null) {
                            storePhoto(uri);
                        }
                    }
                });

        findViewById(R.id.buttonPickPhoto).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                photoPicker.launch(new PickVisualMediaRequest.Builder()
                        .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                        .build());
            }
        });
        buttonRemovePhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                discardPendingPhoto();
                photoPath = null;
                showPhoto(null);
            }
        });
    }

    private void storePhoto(final Uri uri) {
        AppExecutors.get().diskIO().execute(new Runnable() {
            @Override
            public void run() {
                String stored = null;
                try {
                    stored = ImageStore.saveVehiclePhoto(VehicleFormActivity.this, uri);
                } catch (IOException | RuntimeException error) {
                    stored = null;
                }
                final String result = stored;
                AppExecutors.get().mainThread(new Runnable() {
                    @Override
                    public void run() {
                        if (result == null) {
                            Toast.makeText(VehicleFormActivity.this, R.string.garage_photo_error,
                                    Toast.LENGTH_SHORT).show();
                            return;
                        }
                        discardPendingPhoto();
                        photoPath = result;
                        showPhoto(ImageStore.load(result));
                    }
                });
            }
        });
    }

    /** Apaga a cópia feita nesta tela que não chegou a ser salva. */
    private void discardPendingPhoto() {
        if (photoPath != null && !photoPath.equals(originalPhotoPath)) {
            ImageStore.delete(photoPath);
        }
    }

    private void showPhoto(Bitmap bitmap) {
        boolean hasPhoto = bitmap != null;
        buttonRemovePhoto.setVisibility(hasPhoto ? View.VISIBLE : View.GONE);
        if (hasPhoto) {
            imageVehicle.setPadding(0, 0, 0, 0);
            imageVehicle.setImageTintList(null);
            imageVehicle.setImageBitmap(bitmap);
            return;
        }
        int padding = getResources().getDimensionPixelSize(R.dimen.carbon_space_lg);
        imageVehicle.setPadding(padding, padding, padding, padding);
        imageVehicle.setImageResource(R.drawable.ic_car);
        imageVehicle.setImageTintList(ColorStateList.valueOf(
                ContextCompat.getColor(this, R.color.carbon_text_dim)));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Saiu sem salvar: a cópia da foto não deve ficar ocupando espaço.
        if (!saved) {
            discardPendingPhoto();
        }
    }

    private void setupDropdowns() {
        inputFuel.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1, FUELS));
        inputTransmission.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1, TRANSMISSIONS));
        if (vehicleId <= 0) {
            inputFuel.setText(FUELS[0], false);
            inputTransmission.setText(TRANSMISSIONS[0], false);
        }
    }

    private void fill(Vehicle vehicle) {
        inputNickname.setText(vehicle.nickname);
        originalPhotoPath = vehicle.photoPath;
        photoPath = vehicle.photoPath;
        showPhoto(ImageStore.load(vehicle.photoPath));
        inputBrand.setText(vehicle.brand);
        inputModel.setText(vehicle.model);
        inputYear.setText(String.valueOf(vehicle.year));
        inputEngine.setText(vehicle.engine);
        inputKm.setText(String.valueOf(vehicle.currentKm));
        inputFuel.setText(vehicle.fuel, false);
        inputTransmission.setText(vehicle.transmission, false);
    }

    private void submit() {
        clearErrors();
        String brand = text(inputBrand);
        String model = text(inputModel);
        String yearText = text(inputYear);
        String engine = text(inputEngine);
        String kmText = text(inputKm);

        boolean valid = true;
        if (TextUtils.isEmpty(brand)) {
            layoutBrand.setError(getString(R.string.error_required));
            valid = false;
        }
        if (TextUtils.isEmpty(model)) {
            layoutModel.setError(getString(R.string.error_required));
            valid = false;
        }
        if (TextUtils.isEmpty(engine)) {
            layoutEngine.setError(getString(R.string.error_required));
            valid = false;
        }

        int year = 0;
        int currentYear = Calendar.getInstance().get(Calendar.YEAR);
        if (TextUtils.isEmpty(yearText)) {
            layoutYear.setError(getString(R.string.error_required));
            valid = false;
        } else {
            year = Integer.parseInt(yearText);
            if (year < 1900 || year > currentYear + 1) {
                layoutYear.setError(getString(R.string.error_year_invalid));
                valid = false;
            }
        }

        int km = 0;
        if (TextUtils.isEmpty(kmText)) {
            layoutKm.setError(getString(R.string.error_required));
            valid = false;
        } else {
            try {
                km = Integer.parseInt(kmText);
            } catch (NumberFormatException error) {
                km = -1;
            }
            if (km < 0 || km > 2_000_000) {
                layoutKm.setError(getString(R.string.error_km_invalid));
                valid = false;
            }
        }

        if (!valid) {
            return;
        }

        Vehicle vehicle = loaded != null ? loaded : new Vehicle();
        vehicle.id = vehicleId;
        vehicle.brand = brand;
        vehicle.model = model;
        vehicle.year = year;
        vehicle.engine = engine;
        vehicle.fuel = text(inputFuel);
        vehicle.transmission = text(inputTransmission);
        vehicle.currentKm = km;
        vehicle.nickname = text(inputNickname);
        vehicle.photoPath = photoPath;
        viewModel.save(vehicle);
    }

    private void clearErrors() {
        layoutBrand.setError(null);
        layoutModel.setError(null);
        layoutYear.setError(null);
        layoutEngine.setError(null);
        layoutKm.setError(null);
    }

    private String text(android.widget.TextView view) {
        return view.getText() == null ? "" : view.getText().toString().trim();
    }
}
