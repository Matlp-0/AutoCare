package com.example.myapplication.data.repository;

import com.example.myapplication.data.local.AppDatabase;
import com.example.myapplication.data.local.entity.MaintenanceSchedule;
import com.example.myapplication.data.local.entity.RevisionCheck;
import com.example.myapplication.domain.model.UpcomingMaintenance;
import com.example.myapplication.domain.scheduler.MaintenanceScheduler;
import com.example.myapplication.util.AppExecutors;
import com.example.myapplication.util.Callback;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Persiste o snapshot do cronograma calculado (leitura rápida e offline). */
public class ScheduleRepository {

    private final AppDatabase database;

    public ScheduleRepository(AppDatabase database) {
        this.database = database;
    }

    public void persistSync(long vehicleId, MaintenanceScheduler.Result result) {
        if (result == null) {
            return;
        }
        List<MaintenanceSchedule> schedules = new ArrayList<>();
        long now = System.currentTimeMillis();
        for (UpcomingMaintenance item : result.all) {
            MaintenanceSchedule schedule = new MaintenanceSchedule();
            schedule.vehicleId = vehicleId;
            schedule.type = item.type.name();
            schedule.label = item.label;
            schedule.intervalKm = item.intervalKm;
            schedule.intervalMonths = item.intervalMonths;
            schedule.lastDoneKm = item.lastDoneKm;
            schedule.lastDoneDate = item.lastDoneDate;
            schedule.nextDueKm = item.nextDueKm;
            schedule.nextDueDate = item.nextDueDate;
            schedule.status = item.status.name();
            schedule.updatedAt = now;
            schedules.add(schedule);
        }
        database.maintenanceScheduleDao().deleteByVehicle(vehicleId);
        database.maintenanceScheduleDao().insertAll(schedules);
    }

    public List<MaintenanceSchedule> findByVehicleSync(long vehicleId) {
        return database.maintenanceScheduleDao().findByVehicle(vehicleId);
    }

    /** Marcações do usuário: km da revisão -> realizada (true) ou não (false). */
    public Map<Integer, Boolean> findChecksSync(long vehicleId) {
        Map<Integer, Boolean> checks = new HashMap<>();
        for (RevisionCheck check : database.revisionCheckDao().findByVehicle(vehicleId)) {
            checks.put(check.km, check.done);
        }
        return checks;
    }

    public void setRevisionDone(final long vehicleId, final int km, final boolean done,
                                final Callback<Boolean> callback) {
        AppExecutors.get().diskIO().execute(new Runnable() {
            @Override
            public void run() {
                RevisionCheck check = new RevisionCheck();
                check.vehicleId = vehicleId;
                check.km = km;
                check.done = done;
                check.doneDate = done ? System.currentTimeMillis() : 0L;
                check.updatedAt = System.currentTimeMillis();
                database.revisionCheckDao().upsert(check);
                if (callback != null) {
                    AppExecutors.get().mainThread(new Runnable() {
                        @Override
                        public void run() {
                            callback.onResult(done);
                        }
                    });
                }
            }
        });
    }
}
