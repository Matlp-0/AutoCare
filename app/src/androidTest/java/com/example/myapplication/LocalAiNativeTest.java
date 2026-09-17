package com.example.myapplication;

import android.content.Context;
import android.content.Intent;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.example.myapplication.data.ai.LocalModelStore;
import com.example.myapplication.data.ai.NativeLlama;
import com.example.myapplication.data.local.entity.Vehicle;
import com.example.myapplication.domain.ai.MaintenancePrompt;
import com.example.myapplication.domain.scheduler.MaintenanceScheduler;
import com.example.myapplication.ui.assistant.AssistantActivity;

import org.junit.Assume;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class LocalAiNativeTest {
    @Test public void nativeLibraryLoadsAndCancellationIsSafe() {
        try (NativeLlama engine = new NativeLlama()) {
            engine.cancel();
            assertThrows(IllegalStateException.class,
                    () -> engine.generate("/missing.gguf", "test", ignored -> {}));
        }
    }
    @Test public void invalidModelIsReportedWithoutCrashing() {
        try (NativeLlama engine = new NativeLlama()) {
            assertThrows(IllegalStateException.class,
                    () -> engine.generate("/missing.gguf", "test", ignored -> {}));
        }
    }
    @Test public void screenSurvivesRecreationWithoutVehicle() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        try (ActivityScenario<AssistantActivity> scenario = ActivityScenario.launch(
                new Intent(context, AssistantActivity.class))) {
            scenario.onActivity(activity -> assertFalse(activity.findViewById(R.id.buttonAiAsk).isEnabled()));
            scenario.recreate();
            scenario.onActivity(activity -> assertNotNull(activity.findViewById(R.id.buttonAiDownload)));
        }
    }
    @Test public void contextAndScreenUseOnlyTheRequestedVehicle() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        AppContainer container = AutoCareApp.container(context);
        Vehicle first = new Vehicle(); first.brand = "TEST_ONLY_FIRST"; first.model = "Argo";
        Vehicle second = new Vehicle(); second.brand = "TEST_ONLY_SECOND"; second.model = "Outro";
        long firstId = container.database.vehicleDao().insert(first);
        long secondId = container.database.vehicleDao().insert(second);
        try {
            String prompt = new com.example.myapplication.data.ai.AssistantContextReader(container)
                    .prompt(firstId, "Resumo");
            assertTrue(prompt.contains("TEST_ONLY_FIRST"));
            assertFalse(prompt.contains("TEST_ONLY_SECOND"));
            try (ActivityScenario<AssistantActivity> scenario = ActivityScenario.launch(
                    new Intent(context, AssistantActivity.class)
                            .putExtra(AssistantActivity.EXTRA_VEHICLE_ID, firstId))) {
                InstrumentationRegistry.getInstrumentation().waitForIdleSync();
                scenario.onActivity(activity -> {
                    android.widget.TextView label = activity.findViewById(R.id.textAiVehicle);
                    assertTrue(label.getText().toString().contains("TEST_ONLY_FIRST"));
                });
            }
        } finally {
            container.database.vehicleDao().deleteById(firstId);
            container.database.vehicleDao().deleteById(secondId);
        }
    }
    /** Opt-in: put the verified model in app-private storage and pass -e localAiModelTest true. */
    @Test public void realModelProducesAnAnswerAndCanBeCancelled() {
        Assume.assumeTrue("true".equals(InstrumentationRegistry.getArguments().getString("localAiModelTest")));
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        LocalModelStore store = new LocalModelStore(context);
        assertTrue("Import or download the recommended model first", store.isReady());
        Vehicle vehicle = new Vehicle(); vehicle.brand = "Fiat"; vehicle.model = "Argo";
        String prompt = MaintenancePrompt.build(vehicle, Collections.emptyList(),
                new MaintenanceScheduler.Result(), false, System.currentTimeMillis(), "Há manutenções registradas?");
        AtomicReference<String> response = new AtomicReference<>("");
        try (NativeLlama engine = new NativeLlama()) {
            int tokens = engine.generate(store.file().getAbsolutePath(), prompt,
                    bytes -> response.set(new String(bytes, StandardCharsets.UTF_8)));
            assertTrue(tokens > 0);
            assertFalse(response.get().trim().isEmpty());
            assertFalse(response.get().contains("<think>"));
        }
        android.util.Log.i("AutoCareAITest", response.get());
        try (NativeLlama engine = new NativeLlama()) {
            assertThrows(IllegalStateException.class, () -> engine.generate(store.file().getAbsolutePath(), prompt,
                    bytes -> { if (bytes.length > 0) engine.cancel(); }));
        }
    }
}
