package com.example.myapplication.domain.model;

import java.util.ArrayList;
import java.util.List;

/** Indicador de saúde do veículo (0-100). */
public class VehicleHealth {

    public int score;
    public String label;
    public List<String> reasons = new ArrayList<>();

    public VehicleHealth(int score, String label) {
        this.score = score;
        this.label = label;
    }
}
