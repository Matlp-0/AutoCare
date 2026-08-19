package com.example.myapplication.domain.manual;

import com.example.myapplication.data.local.entity.Vehicle;

/**
 * Espaço reservado para a busca do manual via IA (ex.: Claude API com web search),
 * que cobriria modelos sem tabela pública.
 *
 * <p>Não está integrado: exige chave de API, que não deve ser embutida no APK —
 * o caminho seguro é o app chamar um backend próprio que guarda a chave. Enquanto
 * isso, {@link #findManual(Vehicle)} devolve {@code null} e o app segue com o
 * provider web e o plano local.
 */
public class AiVehicleManualProvider implements VehicleManualProvider {

    @Override
    public ManualInfo findManual(Vehicle vehicle) {
        return null;
    }

    public boolean isConfigured() {
        return false;
    }
}
