package com.example.myapplication.util;

import java.text.NumberFormat;
import java.util.Locale;

public final class Formatters {

    private static final Locale BR = new Locale("pt", "BR");

    private Formatters() {
    }

    public static String money(double value) {
        return NumberFormat.getCurrencyInstance(BR).format(value);
    }

    public static String km(int km) {
        return NumberFormat.getIntegerInstance(BR).format(km) + " km";
    }

    public static String kmNumber(int km) {
        return NumberFormat.getIntegerInstance(BR).format(km);
    }

    /** Ex.: "42,5 L". */
    public static String liters(double liters) {
        return decimal(liters, 1, 1) + " L";
    }

    /** Ex.: "12,4 km/l". */
    public static String consumption(double kmPerLiter) {
        if (kmPerLiter <= 0d) {
            return "—";
        }
        return decimal(kmPerLiter, 1, 1) + " km/l";
    }

    /** Preço de bomba tem 3 casas: "R$ 5,899/L". */
    public static String pricePerLiter(double price) {
        if (price <= 0d) {
            return "—";
        }
        return "R$ " + decimal(price, 3, 3) + "/L";
    }

    /** Ex.: "R$ 0,74/km". */
    public static String costPerKm(double cost) {
        if (cost <= 0d) {
            return "—";
        }
        return "R$ " + decimal(cost, 2, 2) + "/km";
    }

    /** Ex.: "38 km/dia". */
    public static String kmPerDay(float kmPerDay) {
        if (kmPerDay <= 0f) {
            return "—";
        }
        return decimal(kmPerDay, 0, 1) + " km/dia";
    }

    public static String decimal(double value, int min, int max) {
        NumberFormat format = NumberFormat.getNumberInstance(BR);
        format.setMinimumFractionDigits(min);
        format.setMaximumFractionDigits(max);
        return format.format(value);
    }

    /** Aceita "1.234,56" (pt-BR) e "1234.56" (teclado simples). */
    public static double parseDecimal(String value) {
        return parseMoney(value);
    }

    public static double parseMoney(String value) {
        if (value == null || value.trim().isEmpty()) {
            return 0d;
        }
        String cleaned = value.trim().replace("R$", "").replace(" ", "");
        if (cleaned.contains(",")) {
            cleaned = cleaned.replace(".", "").replace(",", ".");
        }
        try {
            return Double.parseDouble(cleaned);
        } catch (NumberFormatException error) {
            return 0d;
        }
    }
}
