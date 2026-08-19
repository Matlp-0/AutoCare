package com.example.myapplication.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public final class DateUtils {

    public static final long DAY_MILLIS = 24L * 60L * 60L * 1000L;

    private static final Locale BR = new Locale("pt", "BR");

    private DateUtils() {
    }

    public static String formatShort(long millis) {
        return new SimpleDateFormat("dd/MM/yyyy", BR).format(new Date(millis));
    }

    /** Ex.: "10 AGO 2026" (usado na lista de histórico). */
    public static String formatHistory(long millis) {
        return new SimpleDateFormat("dd MMM yyyy", BR).format(new Date(millis)).toUpperCase(BR);
    }

    public static long parseShort(String value) throws ParseException {
        return new SimpleDateFormat("dd/MM/yyyy", BR).parse(value).getTime();
    }

    public static long addMonths(long millis, int months) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(millis);
        calendar.add(Calendar.MONTH, months);
        return calendar.getTimeInMillis();
    }

    public static int daysBetween(long from, long to) {
        return (int) ((to - from) / DAY_MILLIS);
    }

    /** Texto amigável para prazos: "em aproximadamente 2 meses", "atrasado 12 dias". */
    public static String humanizeDays(int days) {
        int abs = Math.abs(days);
        String amount;
        if (abs < 45) {
            amount = abs + (abs == 1 ? " dia" : " dias");
        } else {
            int months = Math.round(abs / 30f);
            amount = months + (months == 1 ? " mês" : " meses");
        }
        return days < 0 ? "Atrasado há " + amount : "Em aproximadamente " + amount;
    }
}
