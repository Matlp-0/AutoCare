package com.example.myapplication.domain.manual;

import com.example.myapplication.domain.document.MaintenanceInterpreter;
import com.example.myapplication.domain.model.MaintenanceType;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Converte a tabela de revisões de uma página pública em um plano de manutenção.
 *
 * <p>O formato esperado é a matriz usada pelas tabelas de revisão: a primeira linha
 * traz as quilometragens ("10 mil km", "20.000 km") e cada linha seguinte traz um
 * item na primeira coluna e uma marcação nas revisões em que ele é trocado.
 *
 * <p>Sem dependência de Android — testável na JVM.
 */
public class ManualPlanParser {

    /** "10 mil km", "10.000 km", "10000km". */
    private static final Pattern KM_PATTERN = Pattern.compile(
            "(\\d{1,3})\\s*mil\\s*km|(\\d{1,3})[.\\s](\\d{3})\\s*km|(\\d{4,6})\\s*km",
            Pattern.CASE_INSENSITIVE);

    /** ✔ ✓ ☑ X • sim — qualquer marca serve como "trocado nesta revisão". */
    private static final Pattern MARK_PATTERN = Pattern.compile(
            "[✓✔☑●•×xX\\*]|sim", Pattern.CASE_INSENSITIVE);

    private static final int MIN_ENTRIES = 2;
    private static final int MAX_ENTRIES = 8;

    private final MaintenanceInterpreter interpreter;

    public ManualPlanParser(MaintenanceInterpreter interpreter) {
        this.interpreter = interpreter;
    }

    /** Retorna lista vazia quando a página não tem uma tabela de revisões reconhecível. */
    public List<ManualPlanEntry> parse(String html) {
        if (html == null || html.isEmpty()) {
            return new ArrayList<>();
        }
        Document document = Jsoup.parse(html);
        List<ManualPlanEntry> best = new ArrayList<>();
        for (Element table : document.select("table")) {
            List<ManualPlanEntry> entries = parseTable(table);
            if (entries.size() > best.size()) {
                best = entries;
            }
        }
        return best.size() >= MIN_ENTRIES ? best : new ArrayList<ManualPlanEntry>();
    }

    private List<ManualPlanEntry> parseTable(Element table) {
        Elements rows = table.select("tr");
        if (rows.size() < 2) {
            return new ArrayList<>();
        }

        // Cabeçalho: coluna -> quilometragem da revisão.
        Map<Integer, Integer> kmByColumn = new LinkedHashMap<>();
        Elements headerCells = rows.get(0).select("th, td");
        for (int column = 0; column < headerCells.size(); column++) {
            int km = parseKm(headerCells.get(column).text());
            if (km > 0) {
                kmByColumn.put(column, km);
            }
        }
        if (kmByColumn.size() < MIN_ENTRIES) {
            return new ArrayList<>();
        }

        Map<Integer, List<ManualPlanItem>> itemsByKm = new LinkedHashMap<>();
        for (Integer km : kmByColumn.values()) {
            itemsByKm.put(km, new ArrayList<ManualPlanItem>());
        }

        for (int rowIndex = 1; rowIndex < rows.size(); rowIndex++) {
            Elements cells = rows.get(rowIndex).select("td, th");
            if (cells.isEmpty()) {
                continue;
            }
            String label = clean(cells.get(0).text());
            if (label.isEmpty() || parseKm(label) > 0) {
                continue;
            }
            // Um rótulo pode citar mais de um serviço ("Óleo de Motor e Filtro de Óleo").
            List<MaintenanceType> types = interpreter.classifyAll(label);
            if (types.isEmpty()) {
                types = new ArrayList<>();
                types.add(MaintenanceType.OTHER);
            }
            for (Map.Entry<Integer, Integer> header : kmByColumn.entrySet()) {
                int column = header.getKey();
                if (column >= cells.size()) {
                    continue;
                }
                if (isMarked(cells.get(column).text())) {
                    for (MaintenanceType type : types) {
                        itemsByKm.get(header.getValue()).add(new ManualPlanItem(label, type));
                    }
                }
            }
        }

        List<ManualPlanEntry> entries = new ArrayList<>();
        for (Map.Entry<Integer, List<ManualPlanItem>> entry : itemsByKm.entrySet()) {
            if (entry.getValue().isEmpty()) {
                continue;
            }
            int km = entry.getKey();
            entries.add(new ManualPlanEntry(km, monthsFor(km),
                    "Revisão de " + km + " km", entry.getValue()));
            if (entries.size() == MAX_ENTRIES) {
                break;
            }
        }
        return entries;
    }

    /** Regra usual das montadoras: 10.000 km ≈ 12 meses, o que vencer primeiro. */
    private int monthsFor(int km) {
        return Math.max(12, Math.round(km / 10000f) * 12);
    }

    private boolean isMarked(String text) {
        String value = text == null ? "" : text.trim();
        if (value.isEmpty()) {
            return false;
        }
        // Traço/hífen são usados para "não se aplica".
        if (value.equals("-") || value.equals("–") || value.equals("—")) {
            return false;
        }
        return MARK_PATTERN.matcher(value).find();
    }

    private int parseKm(String text) {
        if (text == null) {
            return 0;
        }
        Matcher matcher = KM_PATTERN.matcher(text);
        if (!matcher.find()) {
            return 0;
        }
        int km;
        if (matcher.group(1) != null) {
            km = Integer.parseInt(matcher.group(1)) * 1000;
        } else if (matcher.group(2) != null) {
            km = Integer.parseInt(matcher.group(2) + matcher.group(3));
        } else {
            km = Integer.parseInt(matcher.group(4));
        }
        return km >= 1000 && km <= 300000 ? km : 0;
    }

    private String clean(String text) {
        return text == null ? "" : text.replaceAll("\\s+", " ").trim();
    }
}
