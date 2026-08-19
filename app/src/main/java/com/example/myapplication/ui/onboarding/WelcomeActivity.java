package com.example.myapplication.ui.onboarding;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication.AppContainer;
import com.example.myapplication.AutoCareApp;
import com.example.myapplication.R;
import com.example.myapplication.ui.main.MainActivity;

/** Primeira tela: onboarding ou atalho direto para a Home quando já existe veículo. */
public class WelcomeActivity extends AppCompatActivity {

    private AppContainer container;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        container = AutoCareApp.container(this);

        if (container.preferences.isOnboardingDone()) {
            goToHome();
            return;
        }

        setContentView(R.layout.activity_welcome);

        View start = findViewById(R.id.buttonStart);

        start.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(WelcomeActivity.this, VehicleFormActivity.class));
            }
        });
    }

    private void goToHome() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
