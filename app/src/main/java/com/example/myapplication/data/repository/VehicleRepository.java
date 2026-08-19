package com.example.myapplication.data.repository;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;

import com.example.myapplication.data.local.AppDatabase;
import com.example.myapplication.data.local.entity.OdometerReading;
import com.example.myapplication.data.local.entity.Vehicle;
import com.example.myapplication.util.AppExecutors;
import com.example.myapplication.util.AppPreferences;
import com.example.myapplication.util.ImageStore;
import com.example.myapplication.util.Callback;

import java.util.ArrayList;
import java.util.List;

public class VehicleRepository {

    private final AppDatabase database;
    private final AppPreferences preferences;
    private final AppExecutors executors = AppExecutors.get();

    /** Id escolhido na barra lateral; 0 significa "o primeiro da lista". */
    private final MutableLiveData<Long> activeId = new MutableLiveData<>();
    private final MediatorLiveData<Vehicle> activeVehicle = new MediatorLiveData<>();
    private List<Vehicle> vehicles = new ArrayList<>();

    public VehicleRepository(AppDatabase database, AppPreferences preferences) {
        this.database = database;
        this.preferences = preferences;
        this.activeId.setValue(preferences.getActiveVehicleId());

        // O veículo ativo é resolvido da lista: se o escolhido some (exclusão,
        // banco novo), a tela cai no primeiro em vez de ficar vazia.
        activeVehicle.addSource(database.vehicleDao().observeAll(),
                new Observer<List<Vehicle>>() {
                    @Override
                    public void onChanged(List<Vehicle> all) {
                        vehicles = all == null ? new ArrayList<Vehicle>() : all;
                        resolveActive();
                    }
                });
        activeVehicle.addSource(activeId, new Observer<Long>() {
            @Override
            public void onChanged(Long id) {
                resolveActive();
            }
        });
    }

    private void resolveActive() {
        Long selected = activeId.getValue();
        Vehicle resolved = null;
        if (selected != null && selected > 0L) {
            for (Vehicle vehicle : vehicles) {
                if (vehicle.id == selected) {
                    resolved = vehicle;
                    break;
                }
            }
        }
        if (resolved == null && !vehicles.isEmpty()) {
            resolved = vehicles.get(0);
        }
        Vehicle current = activeVehicle.getValue();
        if (current == null || resolved == null || current.id != resolved.id
                || current.updatedAt != resolved.updatedAt) {
            activeVehicle.setValue(resolved);
        }
    }

    public LiveData<Vehicle> observeActiveVehicle() {
        return activeVehicle;
    }

    public LiveData<List<Vehicle>> observeAll() {
        return database.vehicleDao().observeAll();
    }

    /** Troca o veículo em foco; as telas seguem o LiveData do veículo ativo. */
    public void setActiveVehicle(long vehicleId) {
        preferences.setActiveVehicleId(vehicleId);
        activeId.setValue(vehicleId);
    }

    public long activeVehicleId() {
        Vehicle current = activeVehicle.getValue();
        return current != null ? current.id : preferences.getActiveVehicleId();
    }

    /** Grava a nova ordem da garagem (posição = índice na lista). */
    public void reorder(final List<Long> orderedIds, final Callback<Boolean> callback) {
        executors.diskIO().execute(new Runnable() {
            @Override
            public void run() {
                database.runInTransaction(new Runnable() {
                    @Override
                    public void run() {
                        for (int index = 0; index < orderedIds.size(); index++) {
                            database.vehicleDao().updateSortOrder(orderedIds.get(index), index + 1);
                        }
                    }
                });
                if (callback != null) {
                    executors.mainThread(new Runnable() {
                        @Override
                        public void run() {
                            callback.onResult(true);
                        }
                    });
                }
            }
        });
    }

    /** Remove o veículo e tudo que depende dele (FK em CASCADE). */
    public void delete(final long vehicleId, final Callback<Boolean> callback) {
        executors.diskIO().execute(new Runnable() {
            @Override
            public void run() {
                // A foto vive fora do banco: o CASCADE não a apagaria.
                Vehicle stored = database.vehicleDao().findById(vehicleId);
                if (stored != null) {
                    ImageStore.delete(stored.photoPath);
                }
                database.vehicleDao().deleteById(vehicleId);
                if (preferences.getActiveVehicleId() == vehicleId) {
                    preferences.setActiveVehicleId(0L);
                    executors.mainThread(new Runnable() {
                        @Override
                        public void run() {
                            activeId.setValue(0L);
                        }
                    });
                }
                if (callback != null) {
                    executors.mainThread(new Runnable() {
                        @Override
                        public void run() {
                            callback.onResult(true);
                        }
                    });
                }
            }
        });
    }

    public List<Vehicle> findAllSync() {
        return database.vehicleDao().findAll();
    }

    public Vehicle findActiveSync() {
        long selected = preferences.getActiveVehicleId();
        Vehicle vehicle = selected > 0L ? database.vehicleDao().findById(selected) : null;
        return vehicle != null ? vehicle : database.vehicleDao().findFirst();
    }

    public void hasVehicle(final Callback<Boolean> callback) {
        executors.diskIO().execute(new Runnable() {
            @Override
            public void run() {
                final boolean exists = database.vehicleDao().count() > 0;
                executors.mainThread(new Runnable() {
                    @Override
                    public void run() {
                        callback.onResult(exists);
                    }
                });
            }
        });
    }

    public void save(final Vehicle vehicle, final Callback<Long> callback) {
        executors.diskIO().execute(new Runnable() {
            @Override
            public void run() {
                long now = System.currentTimeMillis();
                vehicle.updatedAt = now;
                final long id;
                // Só registra leitura quando a km muda: editar cor ou placa não é
                // rodagem nova e sujaria a média de km/dia.
                boolean kmChanged = true;
                if (vehicle.id > 0) {
                    Vehicle previous = database.vehicleDao().findById(vehicle.id);
                    kmChanged = previous == null || previous.currentKm != vehicle.currentKm;
                    database.vehicleDao().update(vehicle);
                    id = vehicle.id;
                } else {
                    vehicle.createdAt = now;
                    if (vehicle.sortOrder <= 0) {
                        // Entra no fim da garagem; a ordem depois é arrastada.
                        Integer max = database.vehicleDao().maxSortOrder();
                        vehicle.sortOrder = (max == null ? 0 : max) + 1;
                    }
                    id = database.vehicleDao().insert(vehicle);
                }
                if (kmChanged && vehicle.currentKm > 0) {
                    database.odometerReadingDao().insert(OdometerReading.of(id, now,
                            vehicle.currentKm, OdometerReading.SOURCE_MANUAL));
                }
                notifyResult(callback, id);
            }
        });
    }

    /**
     * Atualiza a km atual e guarda a leitura no histórico do hodômetro, que é o
     * insumo do km/dia usado para projetar datas de manutenção.
     */
    public void updateKm(final long vehicleId, final int km, final Callback<Long> callback) {
        executors.diskIO().execute(new Runnable() {
            @Override
            public void run() {
                final long now = System.currentTimeMillis();
                database.runInTransaction(new Runnable() {
                    @Override
                    public void run() {
                        database.vehicleDao().updateKm(vehicleId, km, now);
                        database.odometerReadingDao().insert(OdometerReading.of(vehicleId, now, km,
                                OdometerReading.SOURCE_MANUAL));
                    }
                });
                notifyResult(callback, vehicleId);
            }
        });
    }

    private void notifyResult(final Callback<Long> callback, final long value) {
        if (callback == null) {
            return;
        }
        executors.mainThread(new Runnable() {
            @Override
            public void run() {
                callback.onResult(value);
            }
        });
    }
}
