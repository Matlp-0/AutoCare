package com.example.myapplication.ui.main;

import android.Manifest;
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import com.example.myapplication.AppContainer;
import com.example.myapplication.AutoCareApp;
import com.example.myapplication.R;
import com.example.myapplication.data.local.entity.Vehicle;
import com.example.myapplication.notification.ReminderScheduler;
import com.example.myapplication.ui.car.CarFragment;
import com.example.myapplication.ui.carbon.KmUpdateDialog;
import com.example.myapplication.ui.fuel.AddRefuelActivity;
import com.example.myapplication.ui.fuel.FuelFragment;
import com.example.myapplication.ui.history.HistoryFragment;
import com.example.myapplication.ui.home.HomeFragment;
import com.example.myapplication.ui.importinvoice.ImportInvoiceActivity;
import com.example.myapplication.ui.maintenance.AddMaintenanceActivity;
import com.example.myapplication.ui.maintenance.AddOptionsBottomSheet;
import com.example.myapplication.ui.onboarding.VehicleFormActivity;
import com.example.myapplication.ui.schedule.ScheduleFragment;
import com.example.myapplication.util.Callback;
import com.example.myapplication.util.Formatters;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationBarView;

import java.util.ArrayList;
import java.util.List;

/** Container das abas + ponto de entrada para adicionar manutenção e abastecimento. */
public class MainActivity extends AppCompatActivity implements AddOptionsBottomSheet.Listener {

    /** Ação pedida por quem abriu a tela (hoje só a notificação de quilometragem). */
    public static final String EXTRA_ACTION = "action";
    public static final String ACTION_UPDATE_KM = "update_km";

    private ActivityResultLauncher<String> notificationPermissionLauncher;
    private DrawerLayout drawerLayout;
    private VehicleViewModel vehicleViewModel;
    private VehicleNavAdapter vehicleAdapter;
    private ItemTouchHelper dragHelper;
    private TextView textActiveVehicle;
    private TextView textActiveVehicleKm;
    private final List<Vehicle> garage = new ArrayList<>();
    private long activeVehicleId;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setupGarage();

        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);
        bottomNav.setOnItemSelectedListener(new NavigationBarView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                return showFragment(item.getItemId());
            }
        });

        FloatingActionButton fab = findViewById(R.id.fabAdd);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new AddOptionsBottomSheet().show(getSupportFragmentManager(), "add_options");
            }
        });

        if (savedInstanceState == null) {
            bottomNav.setSelectedItemId(R.id.nav_home);
        }

        notificationPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                granted -> {
                    // O app continua funcionando offline mesmo sem permissão de notificação.
                });
        requestNotificationPermission();
        ReminderScheduler.scheduleDailyCheck(this);
        handleAction(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleAction(intent);
    }

    /** Toque na notificação de quilometragem: abre a aba e já pede o número. */
    private void handleAction(Intent intent) {
        if (intent == null || !ACTION_UPDATE_KM.equals(intent.getStringExtra(EXTRA_ACTION))) {
            return;
        }
        intent.removeExtra(EXTRA_ACTION);
        openFuelTab();

        final VehicleViewModel viewModel =
                new ViewModelProvider(this).get(VehicleViewModel.class);
        final LiveData<Vehicle> source = viewModel.vehicle();
        source.observe(this, new Observer<Vehicle>() {
            @Override
            public void onChanged(Vehicle vehicle) {
                source.removeObserver(this);
                if (vehicle != null) {
                    KmUpdateDialog.show(MainActivity.this, viewModel, vehicle.currentKm);
                }
            }
        });
    }

    /** Barra lateral: lista de veículos, troca do ativo e entrada para cadastrar. */
    private void setupGarage() {
        drawerLayout = findViewById(R.id.drawerLayout);
        textActiveVehicle = findViewById(R.id.textActiveVehicle);
        textActiveVehicleKm = findViewById(R.id.textActiveVehicleKm);

        findViewById(R.id.buttonMenu).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                drawerLayout.openDrawer(GravityCompat.START);
            }
        });
        findViewById(R.id.buttonAddVehicle).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                drawerLayout.closeDrawer(GravityCompat.START);
                startActivity(new Intent(MainActivity.this, VehicleFormActivity.class));
            }
        });

        vehicleAdapter = new VehicleNavAdapter(new VehicleNavAdapter.Listener() {
            @Override
            public void onSelect(Vehicle vehicle) {
                drawerLayout.closeDrawer(GravityCompat.START);
                if (vehicle.id != activeVehicleId) {
                    vehicleViewModel.selectVehicle(vehicle.id);
                }
            }

            @Override
            public void onLongClick(Vehicle vehicle) {
                showVehicleActions(vehicle);
            }

            @Override
            public void onDragRequested(RecyclerView.ViewHolder holder) {
                dragHelper.startDrag(holder);
            }
        });
        RecyclerView recycler = findViewById(R.id.recyclerVehicles);
        recycler.setLayoutManager(new LinearLayoutManager(this));
        recycler.setAdapter(vehicleAdapter);
        attachDragToReorder(recycler);

        vehicleViewModel = new ViewModelProvider(this).get(VehicleViewModel.class);
        vehicleViewModel.vehicle().observe(this, new Observer<Vehicle>() {
            @Override
            public void onChanged(Vehicle vehicle) {
                activeVehicleId = vehicle == null ? 0L : vehicle.id;
                textActiveVehicle.setText(vehicle == null
                        ? getString(R.string.error_no_vehicle) : vehicle.displayName());
                textActiveVehicleKm.setText(vehicle == null
                        ? "" : Formatters.km(vehicle.currentKm));
                vehicleAdapter.submit(garage, activeVehicleId);
            }
        });
        vehicleViewModel.vehicles().observe(this, new Observer<List<Vehicle>>() {
            @Override
            public void onChanged(List<Vehicle> vehicles) {
                garage.clear();
                if (vehicles != null) {
                    garage.addAll(vehicles);
                }
                vehicleAdapter.submit(garage, activeVehicleId);
            }
        });

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    drawerLayout.closeDrawer(GravityCompat.START);
                    return;
                }
                setEnabled(false);
                getOnBackPressedDispatcher().onBackPressed();
            }
        });
    }

    /**
     * Arrasto pela alça, não por toque longo: o toque longo já abre as ações do
     * veículo, e a ordem só vai para o banco quando o item é solto.
     */
    private void attachDragToReorder(RecyclerView recycler) {
        dragHelper = new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(
                ItemTouchHelper.UP | ItemTouchHelper.DOWN, 0) {
            @Override
            public boolean onMove(@NonNull RecyclerView view,
                                  @NonNull RecyclerView.ViewHolder holder,
                                  @NonNull RecyclerView.ViewHolder target) {
                vehicleAdapter.move(holder.getBindingAdapterPosition(),
                        target.getBindingAdapterPosition());
                return true;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder holder, int direction) {
            }

            @Override
            public boolean isLongPressDragEnabled() {
                return false;
            }

            @Override
            public void clearView(@NonNull RecyclerView view,
                                  @NonNull RecyclerView.ViewHolder holder) {
                super.clearView(view, holder);
                vehicleViewModel.reorderVehicles(vehicleAdapter.currentOrder());
            }
        });
        dragHelper.attachToRecyclerView(recycler);
    }

    private void showVehicleActions(final Vehicle vehicle) {
        String[] options = {getString(R.string.action_edit), getString(R.string.action_delete)};
        new MaterialAlertDialogBuilder(this)
                .setTitle(vehicle.displayName())
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        drawerLayout.closeDrawer(GravityCompat.START);
                        Intent intent = new Intent(this, VehicleFormActivity.class);
                        intent.putExtra(VehicleFormActivity.EXTRA_VEHICLE_ID, vehicle.id);
                        startActivity(intent);
                    } else {
                        confirmDeleteVehicle(vehicle);
                    }
                })
                .show();
    }

    private void confirmDeleteVehicle(final Vehicle vehicle) {
        // Sem veículo o app não tem o que calcular; o último não pode sair.
        if (garage.size() <= 1) {
            Toast.makeText(this, R.string.garage_delete_last, Toast.LENGTH_LONG).show();
            return;
        }
        new MaterialAlertDialogBuilder(this)
                .setTitle(getString(R.string.garage_delete_title, vehicle.displayName()))
                .setMessage(R.string.garage_delete_message)
                .setPositiveButton(R.string.action_delete, (dialog, which) ->
                        vehicleViewModel.deleteVehicle(vehicle.id, new Callback<Boolean>() {
                            @Override
                            public void onResult(Boolean result) {
                                Toast.makeText(MainActivity.this, R.string.garage_deleted,
                                        Toast.LENGTH_SHORT).show();
                            }
                        }))
                .setNegativeButton(R.string.carbon_cancel, null)
                .show();
    }

    private void requestNotificationPermission() {
        AppContainer container = AutoCareApp.container(this);
        if (!container.notifier.hasPermission()) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
        }
    }

    private boolean showFragment(int itemId) {
        Fragment fragment;
        if (itemId == R.id.nav_maintenance) {
            fragment = new HistoryFragment();
        } else if (itemId == R.id.nav_schedule) {
            fragment = new ScheduleFragment();
        } else if (itemId == R.id.nav_fuel) {
            fragment = new FuelFragment();
        } else if (itemId == R.id.nav_car) {
            fragment = new CarFragment();
        } else {
            fragment = new HomeFragment();
        }
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.container, fragment)
                .commit();
        return true;
    }

    /** Usado pela Home ("Ver todas") para pular para a aba Manutenções. */
    public void openMaintenanceTab() {
        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);
        bottomNav.setSelectedItemId(R.id.nav_maintenance);
    }

    /** Usado pela Home ("Ver cronograma") para pular para a aba Revisões. */
    public void openScheduleTab() {
        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);
        bottomNav.setSelectedItemId(R.id.nav_schedule);
    }

    /** Usado pela aba Combustível e pelo menu "+" para abrir o registro de abastecimento. */
    public void openFuelTab() {
        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);
        bottomNav.setSelectedItemId(R.id.nav_fuel);
    }

    @Override
    public void onRefuelSelected() {
        startActivity(new Intent(this, AddRefuelActivity.class));
    }

    @Override
    public void onManualSelected() {
        startActivity(new Intent(this, AddMaintenanceActivity.class));
    }

    @Override
    public void onImportSelected(String action) {
        Intent intent = new Intent(this, ImportInvoiceActivity.class);
        if (action != null) {
            intent.putExtra(ImportInvoiceActivity.EXTRA_ACTION, action);
        }
        startActivity(intent);
    }
}
