package com.example.myapplication.ui.maintenance;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.myapplication.AppContainer;
import com.example.myapplication.AutoCareApp;
import com.example.myapplication.data.local.entity.Document;
import com.example.myapplication.data.local.entity.Maintenance;
import com.example.myapplication.data.local.entity.MaintenanceItem;
import com.example.myapplication.data.local.entity.Vehicle;
import com.example.myapplication.util.AppExecutors;
import com.example.myapplication.util.Callback;

import java.util.List;

/** ViewModel usado pela inserção manual e pela confirmação de nota fiscal. */
public class MaintenanceFormViewModel extends AndroidViewModel {

    private final AppContainer container;
    private final MutableLiveData<Boolean> saved = new MutableLiveData<>();
    private final MutableLiveData<Boolean> deleted = new MutableLiveData<>();
    private final MutableLiveData<Maintenance> editing = new MutableLiveData<>();

    public MaintenanceFormViewModel(@NonNull Application application) {
        super(application);
        container = AutoCareApp.container(application);
    }

    public LiveData<Vehicle> vehicle() {
        return container.vehicleRepository.observeActiveVehicle();
    }

    public LiveData<Boolean> saved() {
        return saved;
    }

    public LiveData<Boolean> deleted() {
        return deleted;
    }

    /** Manutenção carregada para edição. */
    public LiveData<Maintenance> editing() {
        return editing;
    }

    public void load(final long maintenanceId) {
        if (maintenanceId <= 0) {
            return;
        }
        AppExecutors.get().diskIO().execute(new Runnable() {
            @Override
            public void run() {
                editing.postValue(container.database.maintenanceDao().findById(maintenanceId));
            }
        });
    }

    public void update(Maintenance maintenance, List<MaintenanceItem> items) {
        container.maintenanceRepository.update(maintenance, items, new Callback<Long>() {
            @Override
            public void onResult(Long id) {
                saved.setValue(id != null && id > 0);
            }
        });
    }

    public void delete(long maintenanceId) {
        container.maintenanceRepository.deleteById(maintenanceId, new Callback<Boolean>() {
            @Override
            public void onResult(Boolean success) {
                deleted.setValue(Boolean.TRUE.equals(success));
            }
        });
    }

    public void save(Maintenance maintenance, List<MaintenanceItem> items, Document document) {
        container.maintenanceRepository.save(maintenance, items, document, new Callback<Long>() {
            @Override
            public void onResult(Long id) {
                saved.setValue(id != null && id > 0);
            }
        });
    }
}
