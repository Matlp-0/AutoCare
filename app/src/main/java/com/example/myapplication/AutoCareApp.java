package com.example.myapplication;

import android.app.Application;
import android.content.Context;

import com.example.myapplication.ui.carbon.CarbonTheme;

public class AutoCareApp extends Application {

    private AppContainer container;

    @Override
    public void onCreate() {
        super.onCreate();
        container = new AppContainer(this);
        // Aparência escolhida pelo usuário, antes de qualquer tela inflar.
        CarbonTheme.apply(container.preferences.getThemeMode());
        container.notifier.createChannel();
    }

    public AppContainer container() {
        return container;
    }

    public static AppContainer container(Context context) {
        return ((AutoCareApp) context.getApplicationContext()).container();
    }
}
