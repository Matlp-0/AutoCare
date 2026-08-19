package com.example.myapplication;

import android.content.Context;

import com.example.myapplication.data.local.AppDatabase;
import com.example.myapplication.data.remote.HttpFetcher;
import com.example.myapplication.data.repository.FuelRepository;
import com.example.myapplication.data.repository.MaintenanceRepository;
import com.example.myapplication.data.repository.OdometerRepository;
import com.example.myapplication.data.repository.PlanRepository;
import com.example.myapplication.data.repository.ScheduleRepository;
import com.example.myapplication.data.repository.VehicleRepository;
import com.example.myapplication.domain.document.DocumentAnalyzerFactory;
import com.example.myapplication.domain.document.MaintenanceInterpreter;
import com.example.myapplication.domain.document.MlKitOcrEngine;
import com.example.myapplication.domain.fuel.FuelStatsCalculator;
import com.example.myapplication.domain.fuel.KmReminderPolicy;
import com.example.myapplication.domain.fuel.UsageEstimator;
import com.example.myapplication.domain.manual.AiVehicleManualProvider;
import com.example.myapplication.domain.manual.CompositeVehicleManualProvider;
import com.example.myapplication.domain.manual.ManualPlanParser;
import com.example.myapplication.domain.manual.VehicleManualProvider;
import com.example.myapplication.domain.manual.WebVehicleManualProvider;
import com.example.myapplication.domain.scheduler.MaintenanceScheduler;
import com.example.myapplication.domain.scheduler.RevisionTimelineBuilder;
import com.example.myapplication.domain.scheduler.VehicleHealthCalculator;
import com.example.myapplication.notification.MaintenanceNotifier;
import com.example.myapplication.util.AppPreferences;

/**
 * Service locator simples. Troca de implementação (ex.: OCR real, provider remoto)
 * acontece só aqui.
 */
public class AppContainer {

    public final AppDatabase database;
    public final VehicleRepository vehicleRepository;
    public final MaintenanceRepository maintenanceRepository;
    public final PlanRepository planRepository;
    public final ScheduleRepository scheduleRepository;
    public final FuelRepository fuelRepository;
    public final OdometerRepository odometerRepository;
    public final MaintenanceScheduler scheduler;
    public final VehicleHealthCalculator healthCalculator;
    public final RevisionTimelineBuilder timelineBuilder;
    public final FuelStatsCalculator fuelStatsCalculator;
    public final UsageEstimator usageEstimator;
    public final KmReminderPolicy kmReminderPolicy;
    public final MaintenanceInterpreter interpreter;
    public final DocumentAnalyzerFactory analyzerFactory;
    public final AppPreferences preferences;
    public final MaintenanceNotifier notifier;

    public AppContainer(Context context) {
        Context appContext = context.getApplicationContext();
        database = AppDatabase.getInstance(appContext);

        interpreter = new MaintenanceInterpreter();

        // Fontes do plano do fabricante: internet -> IA (não integrada). Sem plano inventado.
        VehicleManualProvider remoteProvider = new CompositeVehicleManualProvider(
                new WebVehicleManualProvider(new HttpFetcher(), new ManualPlanParser(interpreter)),
                new AiVehicleManualProvider());

        preferences = new AppPreferences(appContext);
        vehicleRepository = new VehicleRepository(database, preferences);
        maintenanceRepository = new MaintenanceRepository(database);
        planRepository = new PlanRepository(appContext, database, remoteProvider);
        scheduleRepository = new ScheduleRepository(database);
        fuelRepository = new FuelRepository(database);
        odometerRepository = new OdometerRepository(database);

        scheduler = new MaintenanceScheduler();
        healthCalculator = new VehicleHealthCalculator();
        timelineBuilder = new RevisionTimelineBuilder();
        fuelStatsCalculator = new FuelStatsCalculator();
        usageEstimator = new UsageEstimator();
        kmReminderPolicy = new KmReminderPolicy();

        analyzerFactory = new DocumentAnalyzerFactory(appContext, interpreter,
                new MlKitOcrEngine(appContext));

        notifier = new MaintenanceNotifier(appContext);
    }
}
