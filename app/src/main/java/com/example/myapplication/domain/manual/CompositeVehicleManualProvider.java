package com.example.myapplication.domain.manual;

import com.example.myapplication.data.local.entity.Vehicle;

import java.util.Arrays;
import java.util.List;

/**
 * Encadeia as fontes do plano de manutenção: usa a primeira que responder.
 * Hoje: web (tabela pública) -> IA (não integrada) -> plano padrão genérico.
 */
public class CompositeVehicleManualProvider implements VehicleManualProvider {

    private final List<VehicleManualProvider> providers;

    public CompositeVehicleManualProvider(VehicleManualProvider... providers) {
        this.providers = Arrays.asList(providers);
    }

    @Override
    public ManualInfo findManual(Vehicle vehicle) {
        for (VehicleManualProvider provider : providers) {
            ManualInfo info = provider.findManual(vehicle);
            if (info != null && !info.plan.isEmpty()) {
                return info;
            }
        }
        return null;
    }
}
