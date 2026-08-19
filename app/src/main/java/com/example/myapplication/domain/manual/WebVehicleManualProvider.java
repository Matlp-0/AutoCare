package com.example.myapplication.domain.manual;

import com.example.myapplication.data.local.entity.Vehicle;
import com.example.myapplication.data.remote.HttpFetcher;

import java.io.IOException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Busca o plano de revisões do fabricante na internet a partir de marca/modelo.
 *
 * <p>Estratégia: primeiro a URL previsível da tabela pública de revisões; se o
 * modelo não existir lá, uma busca web restrita ao mesmo site descobre o endereço
 * certo. O que for encontrado é devolvido com a URL de origem para o usuário conferir.
 *
 * <p>Chamada sempre em background e tolerante a falha: sem rede ou sem resultado,
 * quem chama cai no plano local.
 */
public class WebVehicleManualProvider implements VehicleManualProvider {

    private static final String PLAN_URL =
            "https://www.suaoficinaonline.com.br/conteudo/plano-revisao-%s-%s/";
    private static final String SEARCH_URL = "https://html.duckduckgo.com/html/?q=%s";
    private static final Pattern RESULT_PATTERN = Pattern.compile("uddg=([^\"&]+)");
    private static final Pattern PLAN_LINK_PATTERN =
            Pattern.compile("suaoficinaonline\\.com\\.br/conteudo/plano-revisao-[a-z0-9-]+/?");

    private final HttpFetcher fetcher;
    private final ManualPlanParser parser;

    public WebVehicleManualProvider(HttpFetcher fetcher, ManualPlanParser parser) {
        this.fetcher = fetcher;
        this.parser = parser;
    }

    /** @return plano encontrado ou {@code null} quando a busca não deu resultado. */
    @Override
    public ManualInfo findManual(Vehicle vehicle) {
        if (vehicle == null) {
            return null;
        }
        String brand = slug(vehicle.brand);
        String model = slug(vehicle.model);
        if (brand.isEmpty() || model.isEmpty()) {
            return null;
        }

        List<String> candidates = new ArrayList<>();
        candidates.add(String.format(PLAN_URL, brand, model));
        candidates.addAll(searchPlanUrls(vehicle));

        for (String url : candidates) {
            try {
                String html = fetcher.get(url);
                List<ManualPlanEntry> plan = parser.parse(html);
                if (!plan.isEmpty()) {
                    ManualInfo info = new ManualInfo(vehicle.brand + " " + vehicle.model
                            + " " + vehicle.engine + " " + vehicle.year);
                    info.plan = plan;
                    info.source = ManualInfo.SOURCE_REMOTE;
                    info.manualUrl = url;
                    info.exactMatch = true;
                    return info;
                }
            } catch (IOException | RuntimeException error) {
                // Página fora do ar/404/HTML inesperado: tenta o próximo candidato.
            }
        }
        return null;
    }

    /** Descobre o endereço da tabela quando o slug direto não existe. */
    private List<String> searchPlanUrls(Vehicle vehicle) {
        List<String> urls = new ArrayList<>();
        try {
            String query = "site:suaoficinaonline.com.br plano revisao "
                    + vehicle.brand + " " + vehicle.model;
            String html = fetcher.get(String.format(SEARCH_URL,
                    URLEncoder.encode(query, "UTF-8")));
            Matcher matcher = RESULT_PATTERN.matcher(html);
            while (matcher.find() && urls.size() < 3) {
                String decoded = URLDecoder.decode(matcher.group(1), "UTF-8");
                Matcher link = PLAN_LINK_PATTERN.matcher(decoded);
                if (link.find()) {
                    String url = "https://www." + link.group()
                            .replace("www.", "");
                    if (!url.endsWith("/")) {
                        url = url + "/";
                    }
                    // O buscador devolve páginas de outros carros quando o modelo não
                    // existe. Sem o modelo no endereço, o plano não é deste veículo.
                    if (!url.contains(slug(vehicle.model))) {
                        continue;
                    }
                    if (!urls.contains(url)) {
                        urls.add(url);
                    }
                }
            }
        } catch (IOException | RuntimeException error) {
            // Busca indisponível: fica só com a URL direta.
        }
        return urls;
    }

    /** "Volkswagen Gol" -> "volkswagen", "Tiida" -> "tiida". */
    private String slug(String value) {
        if (value == null) {
            return "";
        }
        String normalized = Normalizer.normalize(value.toLowerCase().trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        return normalized.replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-|-$)", "");
    }
}
