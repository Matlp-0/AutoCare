package com.example.myapplication.data.repository;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;

import com.example.myapplication.data.local.AppDatabase;
import com.example.myapplication.data.local.entity.ManufacturerMaintenancePlan;
import com.example.myapplication.data.local.entity.Vehicle;
import com.example.myapplication.domain.manual.ManualInfo;
import com.example.myapplication.domain.manual.ManualPlanEntry;
import com.example.myapplication.domain.manual.ManualPlanItem;
import com.example.myapplication.domain.manual.VehicleManualProvider;
import com.example.myapplication.domain.model.MaintenanceType;
import com.example.myapplication.util.AppExecutors;
import com.example.myapplication.util.Callback;

import java.util.ArrayList;
import java.util.List;

/**
 * Plano de manutenção do fabricante.
 *
 * <p>Ordem de busca: cache no Room -> internet (tabela pública do modelo) -> plano
 * padrão genérico. O que vem da internet é gravado no banco, então o app continua
 * funcionando offline depois da primeira consulta.
 */
public class PlanRepository {

    public static final String SOURCE_WEB = "WEB";

    private final Context context;
    private final AppDatabase database;
    private final VehicleManualProvider remoteProvider;
    private final AppExecutors executors = AppExecutors.get();

    public PlanRepository(Context context, AppDatabase database,
                          VehicleManualProvider remoteProvider) {
        this.context = context.getApplicationContext();
        this.database = database;
        this.remoteProvider = remoteProvider;
    }

    /** Usado pelo motor de manutenção: só o que está no banco, nunca rede. */
    public List<ManualPlanEntry> planForSync(Vehicle vehicle) {
        if (vehicle == null) {
            return new ArrayList<>();
        }
        return storedPlan(vehicle);
    }

    /**
     * Identificação do veículo (tela pós-cadastro): usa o cache quando existir e,
     * se não existir, tenta a internet antes de cair no plano local.
     */
    public void identify(final Vehicle vehicle, final Callback<ManualInfo> callback) {
        executors.diskIO().execute(new Runnable() {
            @Override
            public void run() {
                ManualInfo cached = storedInfo(vehicle);
                if (cached != null) {
                    deliver(callback, cached);
                    return;
                }
                deliver(callback, fetchAndStore(vehicle));
            }
        });
    }

    /** Força uma nova busca online (botão "Buscar manual online"). */
    public void refreshFromWeb(final Vehicle vehicle, final Callback<ManualInfo> callback) {
        executors.diskIO().execute(new Runnable() {
            @Override
            public void run() {
                deliver(callback, fetchAndStore(vehicle));
            }
        });
    }

    public boolean isOnline() {
        ConnectivityManager manager = context.getSystemService(ConnectivityManager.class);
        if (manager == null) {
            return false;
        }
        NetworkCapabilities capabilities =
                manager.getNetworkCapabilities(manager.getActiveNetwork());
        return capabilities != null
                && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
    }

    /**
     * Busca na internet e grava. Se a busca falhar (offline ou modelo sem tabela),
     * devolve o que já estava salvo: um plano oficial nunca é rebaixado para o
     * genérico só porque o usuário tocou em "atualizar" sem conexão.
     */
    private ManualInfo fetchAndStore(Vehicle vehicle) {
        ManualInfo remote = isOnline() ? remoteProvider.findManual(vehicle) : null;
        if (remote != null && !remote.plan.isEmpty()) {
            store(vehicle, remote, SOURCE_WEB);
            return remote;
        }
        // Sem fonte real: devolve o que já estava salvo ou nada. O app não inventa plano.
        return storedInfo(vehicle);
    }

    /** Plano já salvo no banco, junto com a origem de onde ele veio. */
    private ManualInfo storedInfo(Vehicle vehicle) {
        List<ManualPlanEntry> stored = storedPlan(vehicle);
        if (stored.isEmpty()) {
            return null;
        }
        ManualInfo info = new ManualInfo(title(vehicle));
        info.plan = stored;
        info.exactMatch = true;
        ManufacturerMaintenancePlan first = firstStoredRow(vehicle);
        if (first != null && SOURCE_WEB.equals(first.source)) {
            info.source = ManualInfo.SOURCE_REMOTE;
            info.manualUrl = first.sourceUrl;
        }
        return info;
    }

    private void store(Vehicle vehicle, ManualInfo info, String source) {
        List<ManufacturerMaintenancePlan> rows = new ArrayList<>();
        long now = System.currentTimeMillis();
        for (ManualPlanEntry entry : info.plan) {
            ManufacturerMaintenancePlan row = new ManufacturerMaintenancePlan();
            row.brand = vehicle.brand;
            row.model = vehicle.model;
            row.engine = vehicle.engine;
            row.yearFrom = 1900;
            row.yearTo = 2100;
            row.intervalKm = entry.intervalKm;
            row.intervalMonths = entry.intervalMonths;
            row.items = entry.itemsCsv();
            row.itemLabels = entry.labelsCsv();
            row.description = entry.description;
            row.source = source;
            row.sourceUrl = info.manualUrl;
            row.updatedAt = now;
            rows.add(row);
        }
        database.manufacturerPlanDao().deleteForBrandModel(vehicle.brand, vehicle.model);
        database.manufacturerPlanDao().insertAll(rows);
    }

    private List<ManualPlanEntry> storedPlan(Vehicle vehicle) {
        List<ManualPlanEntry> entries = new ArrayList<>();
        for (ManufacturerMaintenancePlan row : storedRows(vehicle)) {
            entries.add(toEntry(row));
        }
        return entries;
    }

    private List<ManufacturerMaintenancePlan> storedRows(Vehicle vehicle) {
        List<ManufacturerMaintenancePlan> rows = database.manufacturerPlanDao()
                .findFor(vehicle.brand, vehicle.model, vehicle.year);
        if (rows == null || rows.isEmpty()) {
            rows = database.manufacturerPlanDao()
                    .findForBrandModel(vehicle.brand, vehicle.model);
        }
        return rows == null ? new ArrayList<ManufacturerMaintenancePlan>() : rows;
    }

    private ManufacturerMaintenancePlan firstStoredRow(Vehicle vehicle) {
        List<ManufacturerMaintenancePlan> rows = storedRows(vehicle);
        return rows.isEmpty() ? null : rows.get(0);
    }

    private ManualPlanEntry toEntry(ManufacturerMaintenancePlan row) {
        List<ManualPlanItem> planItems = new ArrayList<>();
        String[] types = row.items == null ? new String[0] : row.items.split(",");
        String[] labels = row.itemLabels == null
                ? new String[0]
                : row.itemLabels.split("\\" + ManualPlanEntry.LABEL_SEPARATOR, -1);
        for (int index = 0; index < types.length; index++) {
            MaintenanceType type = MaintenanceType.fromName(types[index]);
            String label = index < labels.length && !labels[index].trim().isEmpty()
                    ? labels[index].trim()
                    : type.label();
            planItems.add(new ManualPlanItem(label, type));
        }
        return new ManualPlanEntry(row.intervalKm, row.intervalMonths, row.description, planItems);
    }

    private String title(Vehicle vehicle) {
        return (vehicle.brand + " " + vehicle.model + " " + vehicle.engine + " " + vehicle.year)
                .trim();
    }

    private void deliver(final Callback<ManualInfo> callback, final ManualInfo info) {
        if (callback == null) {
            return;
        }
        executors.mainThread(new Runnable() {
            @Override
            public void run() {
                callback.onResult(info);
            }
        });
    }
}
