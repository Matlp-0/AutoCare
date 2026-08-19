package com.example.myapplication.data.local;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.example.myapplication.data.local.dao.DocumentDao;
import com.example.myapplication.data.local.dao.MaintenanceDao;
import com.example.myapplication.data.local.dao.MaintenanceItemDao;
import com.example.myapplication.data.local.dao.MaintenanceScheduleDao;
import com.example.myapplication.data.local.dao.ManufacturerPlanDao;
import com.example.myapplication.data.local.dao.OdometerReadingDao;
import com.example.myapplication.data.local.dao.RefuelDao;
import com.example.myapplication.data.local.dao.RevisionCheckDao;
import com.example.myapplication.data.local.dao.VehicleDao;
import com.example.myapplication.data.local.entity.Document;
import com.example.myapplication.data.local.entity.Maintenance;
import com.example.myapplication.data.local.entity.MaintenanceItem;
import com.example.myapplication.data.local.entity.MaintenanceSchedule;
import com.example.myapplication.data.local.entity.ManufacturerMaintenancePlan;
import com.example.myapplication.data.local.entity.OdometerReading;
import com.example.myapplication.data.local.entity.Refuel;
import com.example.myapplication.data.local.entity.RevisionCheck;
import com.example.myapplication.data.local.entity.Vehicle;

@Database(
        entities = {
                Vehicle.class,
                Maintenance.class,
                MaintenanceItem.class,
                MaintenanceSchedule.class,
                Document.class,
                ManufacturerMaintenancePlan.class,
                RevisionCheck.class,
                OdometerReading.class,
                Refuel.class
        },
        version = 5,
        exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {

    private static final String NAME = "autocare.db";

    /** v2: plano do fabricante passou a guardar nomes originais e a origem (URL). */
    static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE manufacturer_plans ADD COLUMN itemLabels TEXT");
            database.execSQL("ALTER TABLE manufacturer_plans ADD COLUMN sourceUrl TEXT");
            database.execSQL("ALTER TABLE manufacturer_plans ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT 0");
        }
    };

    private static volatile AppDatabase instance;

    public abstract VehicleDao vehicleDao();

    public abstract MaintenanceDao maintenanceDao();

    public abstract MaintenanceItemDao maintenanceItemDao();

    public abstract MaintenanceScheduleDao maintenanceScheduleDao();

    public abstract DocumentDao documentDao();

    public abstract ManufacturerPlanDao manufacturerPlanDao();

    public abstract RevisionCheckDao revisionCheckDao();

    public abstract OdometerReadingDao odometerReadingDao();

    public abstract RefuelDao refuelDao();

    public static AppDatabase getInstance(Context context) {
        if (instance == null) {
            synchronized (AppDatabase.class) {
                if (instance == null) {
                    instance = build(context.getApplicationContext());
                }
            }
        }
        return instance;
    }

    /** v3: marcação manual de revisão realizada. */
    static final Migration MIGRATION_2_3 = new Migration(2, 3) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("CREATE TABLE IF NOT EXISTS `revision_checks` ("
                    + "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,"
                    + "`vehicleId` INTEGER NOT NULL,"
                    + "`km` INTEGER NOT NULL,"
                    + "`done` INTEGER NOT NULL,"
                    + "`doneDate` INTEGER NOT NULL,"
                    + "`updatedAt` INTEGER NOT NULL,"
                    + "FOREIGN KEY(`vehicleId`) REFERENCES `vehicles`(`id`)"
                    + " ON UPDATE NO ACTION ON DELETE CASCADE )");
            database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS"
                    + " `index_revision_checks_vehicleId_km` ON `revision_checks` (`vehicleId`, `km`)");
        }
    };

    /** v4: leituras de hodômetro (base do km/dia) e abastecimentos. */
    static final Migration MIGRATION_3_4 = new Migration(3, 4) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("CREATE TABLE IF NOT EXISTS `odometer_readings` ("
                    + "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,"
                    + "`vehicleId` INTEGER NOT NULL,"
                    + "`date` INTEGER NOT NULL,"
                    + "`km` INTEGER NOT NULL,"
                    + "`source` TEXT,"
                    + "`createdAt` INTEGER NOT NULL,"
                    + "FOREIGN KEY(`vehicleId`) REFERENCES `vehicles`(`id`)"
                    + " ON UPDATE NO ACTION ON DELETE CASCADE )");
            database.execSQL("CREATE INDEX IF NOT EXISTS"
                    + " `index_odometer_readings_vehicleId` ON `odometer_readings` (`vehicleId`)");

            database.execSQL("CREATE TABLE IF NOT EXISTS `refuels` ("
                    + "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,"
                    + "`vehicleId` INTEGER NOT NULL,"
                    + "`date` INTEGER NOT NULL,"
                    + "`odometerKm` INTEGER NOT NULL,"
                    + "`liters` REAL NOT NULL,"
                    + "`pricePerLiter` REAL NOT NULL,"
                    + "`totalCost` REAL NOT NULL,"
                    + "`fuelType` TEXT,"
                    + "`station` TEXT,"
                    + "`fullTank` INTEGER NOT NULL,"
                    + "`missedPrevious` INTEGER NOT NULL,"
                    + "`notes` TEXT,"
                    + "`createdAt` INTEGER NOT NULL,"
                    + "FOREIGN KEY(`vehicleId`) REFERENCES `vehicles`(`id`)"
                    + " ON UPDATE NO ACTION ON DELETE CASCADE )");
            database.execSQL("CREATE INDEX IF NOT EXISTS"
                    + " `index_refuels_vehicleId` ON `refuels` (`vehicleId`)");

            // Semeia o histórico de km com o que o app já sabia: cada manutenção
            // registrada e a leitura atual do veículo. Sem isso o km/dia começaria
            // do zero para quem já usa o app.
            database.execSQL("INSERT INTO `odometer_readings`"
                    + " (`vehicleId`, `date`, `km`, `source`, `createdAt`)"
                    + " SELECT `vehicleId`, `date`, `odometerKm`, 'MAINTENANCE', `date`"
                    + " FROM `maintenances` WHERE `odometerKm` > 0");
            database.execSQL("INSERT INTO `odometer_readings`"
                    + " (`vehicleId`, `date`, `km`, `source`, `createdAt`)"
                    + " SELECT `id`, `updatedAt`, `currentKm`, 'MANUAL', `updatedAt`"
                    + " FROM `vehicles` WHERE `currentKm` > 0 AND `updatedAt` > 0");
        }
    };

    /** v5: identidade do veículo na garagem (apelido, foto e ordem). */
    static final Migration MIGRATION_4_5 = new Migration(4, 5) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE vehicles ADD COLUMN nickname TEXT");
            database.execSQL("ALTER TABLE vehicles ADD COLUMN photoPath TEXT");
            database.execSQL("ALTER TABLE vehicles ADD COLUMN sortOrder INTEGER NOT NULL DEFAULT 0");
            // Ordem inicial = ordem de cadastro, para a lista não embaralhar.
            database.execSQL("UPDATE vehicles SET sortOrder = id");
        }
    };

    private static AppDatabase build(final Context context) {
        // A tabela de planos do fabricante nasce vazia: ela é o cache do que for
        // baixado da internet, com o plano padrão genérico como fallback.
        return Room.databaseBuilder(context, AppDatabase.class, NAME)
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
                .fallbackToDestructiveMigration(false)
                .build();
    }
}
