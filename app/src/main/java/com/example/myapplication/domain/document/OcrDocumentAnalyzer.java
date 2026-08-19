package com.example.myapplication.domain.document;

import android.net.Uri;

import com.example.myapplication.domain.model.MaintenanceType;
import com.example.myapplication.util.DateUtils;

import java.text.ParseException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Fluxo de PDF/foto: documento -> texto (OCR) -> campos e itens sugeridos ->
 * tela de confirmação.
 *
 * <p>OCR erra: tudo o que sai daqui é sugestão. Nada é gravado sem o usuário
 * confirmar na tela seguinte.
 */
public class OcrDocumentAnalyzer implements DocumentAnalyzer {

    /** "R$ 1.234,56", "1.234,56" ou "310,00". */
    private static final Pattern VALUE_PATTERN =
            Pattern.compile("(?i)(?:r\\$\\s*)?([0-9]{1,3}(?:\\.[0-9]{3})*,[0-9]{2})");
    private static final Pattern DATE_PATTERN =
            Pattern.compile("([0-3]?[0-9]/[0-1]?[0-9]/[0-9]{4})");
    private static final Pattern CNPJ_PATTERN =
            Pattern.compile("([0-9]{2}\\.?[0-9]{3}\\.?[0-9]{3}/?[0-9]{4}-?[0-9]{2})");
    private static final Pattern INVOICE_PATTERN = Pattern.compile(
            "(?i)(?:nfc?-?e|nota fiscal|n[º°o]\\.?|numero|número)\\s*[:.\\-]?\\s*([0-9]{3,9})");
    /** Linha que costuma ser a razão social do emitente. */
    private static final Pattern COMPANY_PATTERN =
            Pattern.compile("(?i)^(.{4,60}?(?:ltda|eireli|me\\b|epp\\b|s\\.?a\\.?|comercio|comércio"
                    + "|auto\\s?center|oficina|pneus|autopec|autopeç).*)$");
    /** Total da nota: rótulos mais confiáveis que "o maior número da página". */
    private static final Pattern TOTAL_PATTERN = Pattern.compile(
            "(?i)(?:valor\\s+total|total\\s+da\\s+nota|total\\s+a\\s+pagar|vl\\.?\\s*total"
                    + "|total\\s*(?:geral)?)\\s*[:\\-]?\\s*(?:r\\$\\s*)?"
                    + "([0-9]{1,3}(?:\\.[0-9]{3})*,[0-9]{2})");

    private final OcrEngine engine;
    private final MaintenanceInterpreter interpreter;
    private final String documentType;

    public OcrDocumentAnalyzer(OcrEngine engine, MaintenanceInterpreter interpreter,
                               String documentType) {
        this.engine = engine;
        this.interpreter = interpreter;
        this.documentType = documentType;
    }

    @Override
    public ExtractedInvoice analyze(Uri uri) throws AnalysisException {
        ExtractedInvoice invoice = new ExtractedInvoice();
        invoice.documentType = documentType;
        invoice.uri = uri.toString();
        invoice.issueDate = System.currentTimeMillis();
        invoice.needsReview = true;

        if (!engine.isAvailable()) {
            invoice.rawText = "";
            return invoice;
        }

        String text = engine.extractText(uri);
        invoice.rawText = text;
        parseText(text, invoice);
        interpreter.interpret(invoice);
        return invoice;
    }

    private void parseText(String text, ExtractedInvoice invoice) {
        if (text == null || text.trim().isEmpty()) {
            return;
        }
        String[] lines = text.split("\\r?\\n");

        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.length() < 3) {
                continue;
            }
            readProduct(trimmed, invoice);
            readCompany(trimmed, invoice);
        }

        readInvoiceNumber(text, invoice);
        readCnpj(text, invoice);
        readDate(text, invoice);
        readTotal(text, invoice);
    }

    /** Linha vira item quando o nome bate com algo do catálogo de manutenção. */
    private void readProduct(String line, ExtractedInvoice invoice) {
        if (interpreter.classify(line) == MaintenanceType.OTHER) {
            return;
        }
        ExtractedProduct product = new ExtractedProduct();
        product.name = cleanProductName(line);
        Matcher value = VALUE_PATTERN.matcher(line);
        double last = 0d;
        while (value.find()) {
            // Em linha de item o último número costuma ser o total daquele item.
            last = toDouble(value.group(1));
        }
        product.totalPrice = last;
        product.unitPrice = last;
        invoice.products.add(product);
    }

    /** Tira códigos e valores da ponta, deixando algo legível na confirmação. */
    private String cleanProductName(String line) {
        String name = line.replaceAll("(?i)r\\$\\s*[0-9.,]+", " ")
                .replaceAll("\\s{2,}", " ")
                .trim();
        return name.length() > 60 ? name.substring(0, 60).trim() : name;
    }

    private void readCompany(String line, ExtractedInvoice invoice) {
        if (invoice.companyName != null) {
            return;
        }
        Matcher matcher = COMPANY_PATTERN.matcher(line);
        if (matcher.find()) {
            invoice.companyName = matcher.group(1).trim();
        }
    }

    private void readInvoiceNumber(String text, ExtractedInvoice invoice) {
        Matcher matcher = INVOICE_PATTERN.matcher(text);
        if (matcher.find()) {
            invoice.invoiceNumber = matcher.group(1);
        }
    }

    private void readCnpj(String text, ExtractedInvoice invoice) {
        Matcher matcher = CNPJ_PATTERN.matcher(text);
        if (matcher.find()) {
            invoice.cnpj = matcher.group(1);
        }
    }

    private void readDate(String text, ExtractedInvoice invoice) {
        Matcher matcher = DATE_PATTERN.matcher(text);
        if (matcher.find()) {
            try {
                invoice.issueDate = DateUtils.parseShort(matcher.group(1));
            } catch (ParseException ignored) {
                // mantém a data atual
            }
        }
    }

    private void readTotal(String text, ExtractedInvoice invoice) {
        Matcher labeled = TOTAL_PATTERN.matcher(text);
        double total = 0d;
        while (labeled.find()) {
            total = Math.max(total, toDouble(labeled.group(1)));
        }
        if (total == 0d) {
            // Sem rótulo reconhecido: o maior valor da página é o palpite menos ruim.
            Matcher any = VALUE_PATTERN.matcher(text);
            while (any.find()) {
                total = Math.max(total, toDouble(any.group(1)));
            }
        }
        invoice.totalValue = total;
    }

    private double toDouble(String value) {
        try {
            return Double.parseDouble(value.replace(".", "").replace(",", "."));
        } catch (NumberFormatException error) {
            return 0d;
        }
    }
}
