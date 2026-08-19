package com.example.myapplication.ui.onboarding;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.myapplication.AppContainer;
import com.example.myapplication.AutoCareApp;
import com.example.myapplication.data.local.entity.Vehicle;
import com.example.myapplication.util.AppExecutors;
import com.example.myapplication.util.Callback;

public class VehicleFormViewModel extends AndroidViewModel {

    private final AppContainer container;
    private final MutableLiveData<Vehicle> vehicle = new MutableLiveData<>();
    private final MutableLiveData<Long> savedId = new MutableLiveData<>();

    public VehicleFormViewModel(@NonNull Application application) {
        super(application);
        container = AutoCareApp.container(application);
    }

    public LiveData<Vehicle> vehicle() {
        return vehicle;
    }

    public LiveData<Long> savedId() {
        return savedId;
    }

    public void load(final long vehicleId) {
        if (vehicleId <= 0) {
            return;
        }
        AppExecutors.get().diskIO().execute(new Runnable() {
            @Override
            public void run() {
                vehicle.postValue(container.database.vehicleDao().findById(vehicleId));
            }
        });
    }

    public void save(Vehicle toSave) {
        container.vehicleRepository.save(toSave, new Callback<Long>() {
            @Override
            public void onResult(Long id) {
                container.preferences.setActiveVehicleId(id);
                savedId.setValue(id);
            }
        });
    }
}
