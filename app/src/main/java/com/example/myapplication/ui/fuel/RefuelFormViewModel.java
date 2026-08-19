package com.example.myapplication.ui.fuel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.myapplication.AppContainer;
import com.example.myapplication.AutoCareApp;
import com.example.myapplication.data.local.entity.Refuel;
import com.example.myapplication.data.local.entity.Vehicle;
import com.example.myapplication.util.AppExecutors;
import com.example.myapplication.util.Callback;

/** ViewModel do formulário de abastecimento (criação e edição). */
public class RefuelFormViewModel extends AndroidViewModel {

    private final AppContainer container;
    private final MutableLiveData<Boolean> saved = new MutableLiveData<>();
    private final MutableLiveData<Boolean> deleted = new MutableLiveData<>();
    private final MutableLiveData<Refuel> editing = new MutableLiveData<>();

    public RefuelFormViewModel(@NonNull Application application) {
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

    public LiveData<Refuel> editing() {
        return editing;
    }

    /** Carrega o abastecimento para edição; ids <= 0 seguem como criação. */
    public void load(final long refuelId) {
        if (refuelId <= 0L) {
            return;
        }
        AppExecutors.get().diskIO().execute(new Runnable() {
            @Override
            public void run() {
                final Refuel refuel = container.database.refuelDao().findById(refuelId);
                AppExecutors.get().mainThread(new Runnable() {
                    @Override
                    public void run() {
                        editing.setValue(refuel);
                    }
                });
            }
        });
    }

    public void save(Refuel refuel) {
        container.fuelRepository.save(refuel, new Callback<Long>() {
            @Override
            public void onResult(Long id) {
                saved.setValue(id != null && id > 0L);
            }
        });
    }

    public void delete(long refuelId) {
        container.fuelRepository.deleteById(refuelId, new Callback<Boolean>() {
            @Override
            public void onResult(Boolean result) {
                deleted.setValue(Boolean.TRUE.equals(result));
            }
        });
    }
}
