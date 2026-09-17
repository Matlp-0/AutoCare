package com.example.myapplication.ui.assistant;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.myapplication.AutoCareApp;
import com.example.myapplication.R;
import com.example.myapplication.data.ai.LocalModelStore;
import com.example.myapplication.ui.carbon.CarbonSystemBars;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public final class AssistantActivity extends AppCompatActivity {
    public static final String EXTRA_VEHICLE_ID = "assistant_vehicle_id";
    private AssistantViewModel model;
    private EditText question;
    private long vehicleId;
    private boolean vehicleExists;
    private final ActivityResultLauncher<String[]> pickModel = registerForActivityResult(
            new ActivityResultContracts.OpenDocument(), uri -> {
                if (uri != null) model.transfer(uri);
            });

    @Override protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_assistant);
        ((MaterialToolbar) findViewById(R.id.toolbar)).setNavigationOnClickListener(v -> finish());
        vehicleId = getIntent().getLongExtra(EXTRA_VEHICLE_ID, -1);
        model = new ViewModelProvider(this).get(AssistantViewModel.class);
        question = findViewById(R.id.editAiQuestion);
        AutoCareApp.container(this).database.vehicleDao().observeAll().observe(this, vehicles -> {
            vehicleExists = false;
            String name = getString(R.string.ai_no_vehicle);
            for (com.example.myapplication.data.local.entity.Vehicle vehicle : vehicles) {
                if (vehicle.id == vehicleId) { name = vehicle.displayName(); vehicleExists = true; break; }
            }
            ((TextView) findViewById(R.id.textAiVehicle)).setText(name);
            if (!vehicleExists) model.cancel();
            render(model.state().getValue());
        });
        model.state().observe(this, this::render);
        findViewById(R.id.buttonAiDownload).setOnClickListener(v -> model.transfer(null));
        findViewById(R.id.buttonAiImport).setOnClickListener(v -> pickModel.launch(new String[]{"*/*"}));
        findViewById(R.id.buttonAiSource).setOnClickListener(v -> {
            try { startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(LocalModelStore.MODEL_PAGE))); }
            catch (ActivityNotFoundException e) { Toast.makeText(this, R.string.ai_no_browser, Toast.LENGTH_SHORT).show(); }
        });
        findViewById(R.id.buttonAiDelete).setOnClickListener(v -> new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.ai_delete).setMessage(R.string.ai_delete_message)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.ai_delete, (dialog, which) -> model.deleteModel()).show());
        findViewById(R.id.buttonAiSchedule).setOnClickListener(v -> ask(getString(R.string.ai_schedule_question)));
        findViewById(R.id.buttonAiHistory).setOnClickListener(v -> ask(getString(R.string.ai_history_question)));
        findViewById(R.id.buttonAiAsk).setOnClickListener(v -> ask(question.getText().toString()));
        findViewById(R.id.buttonAiCancel).setOnClickListener(v -> model.cancel());
    }
    private void ask(String text) {
        if (text.trim().isEmpty()) { question.setError(getString(R.string.ai_question_hint)); return; }
        question.setText(text);
        if (vehicleExists) model.ask(vehicleId, text);
    }
    private void render(AssistantViewModel.State state) {
        if (state == null) return;
        ((TextView) findViewById(R.id.textAiStatus)).setText(state.status);
        ((TextView) findViewById(R.id.textAiAnswer)).setText(state.answer);
        findViewById(R.id.progressAi).setVisibility(state.busy ? View.VISIBLE : View.GONE);
        findViewById(R.id.buttonAiCancel).setVisibility(state.busy ? View.VISIBLE : View.GONE);
        findViewById(R.id.buttonAiDownload).setVisibility(state.ready ? View.GONE : View.VISIBLE);
        findViewById(R.id.buttonAiImport).setVisibility(state.ready ? View.GONE : View.VISIBLE);
        findViewById(R.id.buttonAiDelete).setVisibility(state.ready ? View.VISIBLE : View.GONE);
        int[] modelActions = {R.id.buttonAiDownload, R.id.buttonAiImport, R.id.buttonAiDelete, R.id.buttonAiSource};
        for (int id : modelActions) findViewById(id).setEnabled(!state.busy);
        int[] questions = {R.id.buttonAiAsk, R.id.buttonAiSchedule, R.id.buttonAiHistory, R.id.editAiQuestion};
        for (int id : questions) findViewById(id).setEnabled(!state.busy && state.ready && vehicleExists);
    }
    @Override protected void onResume() {
        super.onResume();
        CarbonSystemBars.apply(this);
    }
    @Override protected void onStop() {
        super.onStop();
        if (!isChangingConfigurations() && model != null && model.isBusy()) model.cancel();
    }
}
