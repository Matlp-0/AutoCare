package com.example.myapplication;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;

import androidx.room.Room;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.example.myapplication.data.local.AppDatabase;
import com.example.myapplication.data.local.entity.Document;
import com.example.myapplication.data.local.entity.Maintenance;
import com.example.myapplication.data.local.entity.MaintenanceItem;
import com.example.myapplication.data.local.entity.Vehicle;
import com.example.myapplication.data.repository.MaintenanceRepository;
import com.example.myapplication.domain.model.MaintenanceType;
import com.example.myapplication.util.Callback;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@RunWith(AndroidJUnit4.class)
public class MaintenanceEditDeleteTest {

    private AppDatabase database;
    private MaintenanceRepository repository;
    private long vehicleId;

    @Before
    public void setUp() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase.class)
                .allowMainThreadQueries()
                .build();
        repository = new MaintenanceRepository(database);

        Vehicle vehicle = new Vehicle();
        vehicle.brand = "Nissan";
        vehicle.model = "Tiida";
        vehicle.engine = "1.8";
        vehicle.year = 2009;
        vehicle.currentKm = 147000;
        vehicleId = database.vehicleDao().insert(vehicle);
    }

    @After
    public void tearDown() {
        database.close();
    }

    @Test
    public void updateKeepsItemsWhenListIsNull() throws Exception {
        long id = insertMaintenanceWithItem();

        Maintenance stored = database.maintenanceDao().findById(id);
        stored.description = "Troca de óleo revisada";
        stored.totalCost = 350.00;
        stored.odometerKm = 148000;
        await(latch -> repository.update(stored, null, result -> latch.countDown()));

        Maintenance updated = database.maintenanceDao().findById(id);
        assertEquals("Troca de óleo revisada", updated.description);
        assertEquals(350.00, updated.totalCost, 0.001);
        assertEquals(1, database.maintenanceItemDao().findByMaintenance(id).size());
        // Hodômetro maior atualiza a quilometragem do veículo.
        assertEquals(148000, database.vehicleDao().findById(vehicleId).currentKm);
    }

    @Test
    public void updateReplacesItemsWhenListIsProvided() throws Exception {
        long id = insertMaintenanceWithItem();

        Maintenance stored = database.maintenanceDao().findById(id);
        List<MaintenanceItem> items = new ArrayList<>();
        MaintenanceItem item = new MaintenanceItem();
        item.name = "Filtro de ar";
        item.type = MaintenanceType.AIR_FILTER.name();
        item.totalPrice = 120.00;
        items.add(item);

        await(latch -> repository.update(stored, items, result -> latch.countDown()));

        List<MaintenanceItem> saved = database.maintenanceItemDao().findByMaintenance(id);
        assertEquals(1, saved.size());
        assertEquals("Filtro de ar", saved.get(0).name);
    }

    @Test
    public void deleteRemovesItemsAndDocuments() throws Exception {
        long id = insertMaintenanceWithItem();

        Document document = new Document();
        document.maintenanceId = id;
        document.type = Document.TYPE_XML;
        document.invoiceNumber = "123";
        database.documentDao().insert(document);

        assertNotNull(database.maintenanceDao().findById(id));

        await(latch -> repository.deleteById(id, result -> latch.countDown()));

        assertNull(database.maintenanceDao().findById(id));
        assertTrue(database.maintenanceItemDao().findByMaintenance(id).isEmpty());
        assertTrue(database.documentDao().findByMaintenance(id).isEmpty());
    }

    private long insertMaintenanceWithItem() throws Exception {
        Maintenance maintenance = new Maintenance();
        maintenance.vehicleId = vehicleId;
        maintenance.date = System.currentTimeMillis();
        maintenance.odometerKm = 145000;
        maintenance.category = MaintenanceType.OIL_CHANGE.name();
        maintenance.description = "Troca de óleo";
        maintenance.totalCost = 280.00;

        MaintenanceItem item = new MaintenanceItem();
        item.name = "Óleo 5W30";
        item.type = MaintenanceType.OIL_CHANGE.name();
        item.totalPrice = 180.00;

        List<MaintenanceItem> items = new ArrayList<>();
        items.add(item);

        final long[] created = new long[1];
        final CountDownLatch latch = new CountDownLatch(1);
        repository.save(maintenance, items, null, new Callback<Long>() {
            @Override
            public void onResult(Long id) {
                created[0] = id;
                latch.countDown();
            }
        });
        assertTrue(latch.await(5, TimeUnit.SECONDS));
        return created[0];
    }

    private interface Operation {
        void run(CountDownLatch latch);
    }

    private void await(Operation operation) throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        operation.run(latch);
        assertTrue("operação não terminou a tempo", latch.await(5, TimeUnit.SECONDS));
    }
}
