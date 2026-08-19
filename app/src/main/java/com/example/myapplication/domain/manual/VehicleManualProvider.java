package com.example.myapplication.domain.manual;

import com.example.myapplication.data.local.entity.Vehicle;

/**
 * Fonte do manual do proprietário / plano de manutenção.
 * Implementações: {@link WebVehicleManualProvider} (tabela pública do modelo) e
 * {@link AiVehicleManualProvider} (não integrada).
 *
 * <p>Não existe plano inventado como fallback: quando nenhuma fonte responde, o
 * app fica sem cronograma até conseguir os dados reais do veículo.</p>
 */
public interface VehicleManualProvider {

    ManualInfo findManual(Vehicle vehicle);
}
