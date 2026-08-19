package com.example.myapplication.ui.onboarding;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.AppContainer;
import com.example.myapplication.AutoCareApp;
import com.example.myapplication.R;
import com.example.myapplication.data.local.entity.Vehicle;
import com.example.myapplication.domain.manual.ManualInfo;
import com.example.myapplication.ui.main.MainActivity;
import com.example.myapplication.util.AppExecutors;
import com.example.myapplication.util.Callback;
import com.google.android.material.progressindicator.CircularProgressIndicator;

import java.net.URI;

/**
 * Identificação do veículo: busca na internet a tabela de revisões da marca/modelo
 * e mostra o plano encontrado (com a fonte). Sem internet ou sem resultado, cai no
 * plano local — o usuário nunca fica travado.
 */
public class VehicleIdentificationActivity extends AppCompatActivity {

    public static final String EXTRA_VEHICLE_ID = "vehicle_id";

    private final PlanAdapter adapter = new PlanAdapter();

    private AppContainer container;
    private Vehicle vehicle;

    private TextView textStatus;
    private TextView textSource;
    private CircularProgressIndicator progress;
    private TextView confirm;
    private TextView searchOnline;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_vehicle_identification);

        container = AutoCareApp.container(this);
        final long vehicleId = getIntent().getLongExtra(EXTRA_VEHICLE_ID, 0L);

        final TextView textVehicle = findViewById(R.id.textVehicle);
        textStatus = findViewById(R.id.textStatus);
        textSource = findViewById(R.id.textSource);
        progress = findViewById(R.id.progress);
        confirm = findViewById(R.id.buttonConfirm);
        searchOnline = findViewById(R.id.buttonSearchOnline);

        RecyclerView recycler = findViewById(R.id.recyclerPlan);
        recycler.setLayoutManager(new LinearLayoutManager(this));
        recycler.setAdapter(adapter);

        confirm.setEnabled(false);
        confirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                container.preferences.setOnboardingDone(true);
                Intent intent = new Intent(VehicleIdentificationActivity.this, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            }
        });
        searchOnline.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (vehicle != null) {
                    showLoading(true);
                    container.planRepository.refreshFromWeb(vehicle, resultCallback());
                }
            }
        });

        AppExecutors.get().diskIO().execute(new Runnable() {
            @Override
            public void run() {
                final Vehicle loaded = container.database.vehicleDao().findById(vehicleId);
                AppExecutors.get().mainThread(new Runnable() {
                    @Override
                    public void run() {
                        if (loaded == null) {
                            finish();
                            return;
                        }
                        vehicle = loaded;
                        textVehicle.setText(loaded.displayName() + " " + loaded.engine
                                + " " + loaded.year);
                        showLoading(true);
                        container.planRepository.identify(loaded, resultCallback());
                    }
                });
            }
        });
    }

    private Callback<ManualInfo> resultCallback() {
        return new Callback<ManualInfo>() {
            @Override
            public void onResult(ManualInfo info) {
                showLoading(false);
                confirm.setEnabled(true);
                if (info == null || info.plan.isEmpty()) {
                    adapter.submit(null);
                    textSource.setText("");
                    textStatus.setText(container.planRepository.isOnline()
                            ? R.string.identification_no_plan : R.string.identification_offline);
                    return;
                }
                adapter.submit(info.plan);
                textStatus.setText(getString(R.string.identification_plan_items,
                        info.plan.size()));
                textSource.setText(info.manualUrl != null
                        ? getString(R.string.identification_source_web, host(info.manualUrl))
                        : "");
            }
        };
    }

    private void showLoading(boolean loading) {
        progress.setVisibility(loading ? View.VISIBLE : View.GONE);
        searchOnline.setEnabled(!loading);
        if (loading) {
            textStatus.setText(R.string.identification_searching);
        }
    }

    private String host(String url) {
        try {
            String host = URI.create(url).getHost();
            return host == null ? url : host.replaceFirst("^www\\.", "");
        } catch (IllegalArgumentException error) {
            return url;
        }
    }
}
