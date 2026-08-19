package com.example.myapplication.domain.document;

import com.example.myapplication.domain.model.MaintenanceType;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Traduz descrições de produtos/serviços em itens de manutenção.
 * Regras locais por palavra-chave no MVP — ponto de extensão para IA depois.
 */
public class MaintenanceInterpreter {

    private static final Map<MaintenanceType, List<String>> KEYWORDS = new LinkedHashMap<>();

    // A ordem importa: termos mais específicos vêm antes para não serem "comidos"
    // pelos genéricos (ex.: filtro de ar-condicionado antes de filtro de ar).
    static {
        KEYWORDS.put(MaintenanceType.OIL_FILTER, Arrays.asList("filtro de oleo", "filtro oleo", "filtro lubrificante"));
        KEYWORDS.put(MaintenanceType.CABIN_FILTER, Arrays.asList("filtro de cabine", "filtro de ar condicionado", "filtro ar condicionado", "filtro antipolen", "ar condicionado"));
        KEYWORDS.put(MaintenanceType.FUEL_FILTER, Arrays.asList("filtro de combustivel", "filtro gasolina", "filtro diesel"));
        KEYWORDS.put(MaintenanceType.AIR_FILTER, Arrays.asList("filtro de ar", "filtro ar motor"));
        KEYWORDS.put(MaintenanceType.TRANSMISSION_OIL, Arrays.asList("oleo do cambio", "oleo cambio", "oleo da transmissao", "oleo transmissao", "fluido de transmissao", "atf"));
        KEYWORDS.put(MaintenanceType.OIL_CHANGE, Arrays.asList("oleo de motor", "oleo do motor", "oleo motor", "oleo lubrificante", "troca de oleo", "5w30", "5w40", "10w40", "15w40", "lubrificante"));
        KEYWORDS.put(MaintenanceType.SPARK_PLUGS, Arrays.asList("vela", "velas de ignicao", "cabo de vela"));
        KEYWORDS.put(MaintenanceType.BRAKE_PADS, Arrays.asList("pastilha", "lona de freio", "sapata"));
        KEYWORDS.put(MaintenanceType.BRAKE_DISCS, Arrays.asList("disco de freio", "tambor"));
        KEYWORDS.put(MaintenanceType.BRAKE_FLUID, Arrays.asList("fluido de freio", "dot 4", "dot4"));
        KEYWORDS.put(MaintenanceType.COOLANT, Arrays.asList("aditivo radiador", "aditivo do radiador", "liquido do radiador", "fluido de arrefecimento", "liquido de arrefecimento", "arrefecimento", "anticongelante"));
        KEYWORDS.put(MaintenanceType.BELTS, Arrays.asList("correia", "correia dentada", "tensor"));
        KEYWORDS.put(MaintenanceType.BATTERY, Arrays.asList("bateria"));
        KEYWORDS.put(MaintenanceType.TIRE_ROTATION, Arrays.asList("rodizio"));
        KEYWORDS.put(MaintenanceType.TIRES, Arrays.asList("pneu"));
        KEYWORDS.put(MaintenanceType.ALIGNMENT, Arrays.asList("alinhamento", "balanceamento", "cambagem"));
        KEYWORDS.put(MaintenanceType.SUSPENSION, Arrays.asList("amortecedor", "batente", "coxim", "bandeja", "pivo"));
        KEYWORDS.put(MaintenanceType.GENERAL_INSPECTION, Arrays.asList("revisao", "inspecao", "mao de obra", "servico"));
    }

    /** Descobre o item de manutenção de um produto pelo nome. */
    public MaintenanceType classify(String productName) {
        List<MaintenanceType> found = classifyAll(productName);
        return found.isEmpty() ? MaintenanceType.OTHER : found.get(0);
    }

    /**
     * Todos os itens citados em um texto. Um rótulo de manual costuma juntar mais
     * de um serviço ("Óleo de Motor e Filtro de Óleo"), e ambos precisam entrar
     * no cronograma. Cada trecho reconhecido é removido para não casar duas vezes.
     */
    public List<MaintenanceType> classifyAll(String text) {
        List<MaintenanceType> found = new ArrayList<>();
        String remaining = normalize(text);
        if (remaining.isEmpty()) {
            return found;
        }
        for (Map.Entry<MaintenanceType, List<String>> entry : KEYWORDS.entrySet()) {
            for (String keyword : entry.getValue()) {
                if (remaining.contains(keyword)) {
                    found.add(entry.getKey());
                    remaining = remaining.replace(keyword, " ");
                    break;
                }
            }
        }
        return found;
    }

    /** Classifica os produtos e sugere categoria/descrição da manutenção. */
    public void interpret(ExtractedInvoice invoice) {
        if (invoice == null) {
            return;
        }
        List<MaintenanceType> found = new ArrayList<>();
        for (ExtractedProduct product : invoice.products) {
            product.suggestedType = classify(product.name);
            if (product.suggestedType != MaintenanceType.OTHER && !found.contains(product.suggestedType)) {
                found.add(product.suggestedType);
            }
        }
        if (found.isEmpty() && invoice.rawText != null) {
            MaintenanceType fromText = classify(invoice.rawText);
            if (fromText != MaintenanceType.OTHER) {
                found.add(fromText);
            }
        }
        invoice.suggestedType = found.isEmpty() ? MaintenanceType.OTHER : found.get(0);
        invoice.suggestedDescription = buildDescription(found);
    }

    private String buildDescription(List<MaintenanceType> types) {
        if (types.isEmpty()) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        for (MaintenanceType type : types) {
            if (builder.length() > 0) {
                builder.append(" + ");
            }
            builder.append(type.label());
        }
        return builder.toString();
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }
        String normalized = Normalizer.normalize(value.toLowerCase(), Normalizer.Form.NFD);
        return normalized.replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
                .replaceAll("[^a-z0-9 ]", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }
}
