package com.example.myapplication.util;

import android.os.Handler;
import android.os.Looper;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Executores compartilhados do app. Mantém as escritas do Room fora da main thread
 * sem espalhar criação de threads pelas telas.
 */
public final class AppExecutors {

    private static final AppExecutors INSTANCE = new AppExecutors();

    private final Executor diskIO = Executors.newSingleThreadExecutor();
    private final Executor computation = Executors.newFixedThreadPool(2);
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private AppExecutors() {
    }

    public static AppExecutors get() {
        return INSTANCE;
    }

    public Executor diskIO() {
        return diskIO;
    }

    public Executor computation() {
        return computation;
    }

    public void mainThread(Runnable runnable) {
        mainHandler.post(runnable);
    }
}
