package com.example.myapplication.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "vehicles")
public class Vehicle {

    @PrimaryKey(autoGenerate = true)
    public long id;

    @NonNull
    public String brand = "";

    @NonNull
    public String model = "";

    public int year;

    @NonNull
    public String engine = "";

    @NonNull
    public String fuel = "";

    @NonNull
    public String transmission = "";

    public int currentKm;

    public String plate;

    /** Apelido dado pelo usuário ("Carro da firma"); vazio usa marca + modelo. */
    public String nickname;

    /** Caminho do arquivo da foto dentro do app; nulo quando não há foto. */
    public String photoPath;

    /** Posição na garagem, definida arrastando a lista. */
    public int sortOrder;

    public long createdAt;

    public long updatedAt;

    /** Campos preparados para sincronização futura com backend. */
    public String remoteId;

    public boolean synced;

    /** Nome mostrado ao usuário: o apelido manda quando existe. */
    public String displayName() {
        return nickname != null && !nickname.trim().isEmpty() ? nickname.trim() : technicalName();
    }

    /** Marca + modelo, sempre. Usado onde o apelido não pode esconder o carro real. */
    public String technicalName() {
        return brand + " " + model;
    }

    public boolean hasNickname() {
        return nickname != null && !nickname.trim().isEmpty();
    }

    /** Subtítulo técnico do painel: "2009 • 1.8 • MANUAL". */
    public String displaySpecs() {
        StringBuilder specs = new StringBuilder().append(year);
        if (engine != null && !engine.isEmpty()) {
            specs.append(" • ").append(engine);
        }
        if (transmission != null && !transmission.isEmpty()) {
            specs.append(" • ").append(transmission);
        }
        return specs.toString();
    }
}
