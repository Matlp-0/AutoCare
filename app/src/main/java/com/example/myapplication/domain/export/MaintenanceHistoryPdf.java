package com.example.myapplication.domain.export;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;

import androidx.annotation.Nullable;

import com.example.myapplication.data.local.entity.Maintenance;
import com.example.myapplication.data.local.entity.MaintenanceItem;
import com.example.myapplication.data.local.entity.Vehicle;
import com.example.myapplication.data.local.relation.MaintenanceWithItems;
import com.example.myapplication.domain.model.MaintenanceType;
import com.example.myapplication.util.DateUtils;
import com.example.myapplication.util.Formatters;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Gera o histórico completo de manutenções em PDF e grava na pasta Downloads
 * do aparelho (MediaStore, sem permissão extra a partir do Android 10).
 *
 * <p>O documento é montado com {@link PdfDocument} em páginas A4 (72 dpi),
 * quebrando a página sempre que o próximo bloco não couber.
 */
public final class MaintenanceHistoryPdf {

    private static final int PAGE_WIDTH = 595;
    private static final int PAGE_HEIGHT = 842;
    private static final float MARGIN = 40f;
    private static final float CONTENT_WIDTH = PAGE_WIDTH - (MARGIN * 2f);
    private static final float BOTTOM_LIMIT = PAGE_HEIGHT - MARGIN - 18f;

    private static final int INK = Color.parseColor("#111111");
    private static final int MUTED = Color.parseColor("#666666");
    private static final int RULE = Color.parseColor("#CCCCCC");

    private final Paint title = text(18f, Typeface.DEFAULT_BOLD, INK);
    private final Paint subtitle = text(10f, Typeface.DEFAULT, MUTED);
    private final Paint sectionLabel = text(9f, Typeface.DEFAULT_BOLD, MUTED);
    private final Paint entryTitle = text(12f, Typeface.DEFAULT_BOLD, INK);
    private final Paint body = text(10f, Typeface.DEFAULT, INK);
    private final Paint bodyMuted = text(10f, Typeface.DEFAULT, MUTED);
    private final Paint value = text(11f, Typeface.DEFAULT_BOLD, INK);
    private final Paint footer = text(8f, Typeface.DEFAULT, MUTED);
    private final Paint rule = new Paint();

    private final PdfDocument document = new PdfDocument();
    private final String headerLine;

    private PdfDocument.Page page;
    private Canvas canvas;
    private float cursorY;
    private int pageNumber;

    private MaintenanceHistoryPdf(@Nullable Vehicle vehicle) {
        rule.setColor(RULE);
        rule.setStrokeWidth(0.8f);
        headerLine = vehicleLine(vehicle);
    }

    /**
     * Monta o PDF e devolve a Uri do arquivo salvo em Downloads.
     *
     * @throws IOException quando o MediaStore recusa a escrita.
     */
    public static Uri exportToDownloads(Context context, @Nullable Vehicle vehicle,
                                        List<MaintenanceWithItems> history) throws IOException {
        MaintenanceHistoryPdf builder = new MaintenanceHistoryPdf(vehicle);
        PdfDocument document = builder.build(vehicle, history);
        try {
            return write(context, document, fileName(vehicle));
        } finally {
            document.close();
        }
    }

    private PdfDocument build(@Nullable Vehicle vehicle, List<MaintenanceWithItems> history) {
        List<MaintenanceWithItems> entries = new ArrayList<>(history);
        Collections.sort(entries, new Comparator<MaintenanceWithItems>() {
            @Override
            public int compare(MaintenanceWithItems left, MaintenanceWithItems right) {
                return Long.compare(right.maintenance.date, left.maintenance.date);
            }
        });

        startPage();
        drawCover(vehicle, entries);
        for (MaintenanceWithItems entry : entries) {
            drawEntry(entry);
        }
        if (entries.isEmpty()) {
            cursorY += 8f;
            drawWrapped("Nenhuma manutenção registrada até agora.", bodyMuted, MARGIN, CONTENT_WIDTH);
        }
        finishPage();
        return document;
    }

    // ---------------------------------------------------------------- conteúdo

    private void drawCover(@Nullable Vehicle vehicle, List<MaintenanceWithItems> entries) {
        double total = 0d;
        long first = 0L;
        long last = 0L;
        for (MaintenanceWithItems entry : entries) {
            total += entry.maintenance.totalCost;
            long date = entry.maintenance.date;
            if (first == 0L || date < first) {
                first = date;
            }
            if (date > last) {
                last = date;
            }
        }

        canvas.drawText("Histórico de manutenções", MARGIN, cursorY + 14f, title);
        cursorY += 24f;
        canvas.drawText(headerLine, MARGIN, cursorY, subtitle);
        cursorY += 14f;
        canvas.drawText("Emitido em " + DateUtils.formatShort(System.currentTimeMillis()),
                MARGIN, cursorY, subtitle);
        cursorY += 18f;
        drawRule();

        cursorY += 6f;
        drawPair("REGISTROS", String.valueOf(entries.size()));
        drawPair("TOTAL GASTO", Formatters.money(total));
        if (vehicle != null && vehicle.currentKm > 0) {
            drawPair("ODÔMETRO ATUAL", Formatters.km(vehicle.currentKm));
        }
        if (first > 0L) {
            drawPair("PERÍODO",
                    DateUtils.formatShort(first) + " — " + DateUtils.formatShort(last));
        }
        cursorY += 6f;
        drawRule();
        cursorY += 10f;
    }

    private void drawEntry(MaintenanceWithItems entry) {
        Maintenance maintenance = entry.maintenance;
        ensureSpace(64f);

        canvas.drawText(DateUtils.formatHistory(maintenance.date), MARGIN, cursorY, sectionLabel);
        String cost = Formatters.money(maintenance.totalCost);
        canvas.drawText(cost, MARGIN + CONTENT_WIDTH - value.measureText(cost), cursorY, value);
        cursorY += 14f;

        String description = isEmpty(maintenance.description)
                ? typeLabel(maintenance.category) : maintenance.description;
        drawWrapped(description, entryTitle, MARGIN, CONTENT_WIDTH);
        cursorY += 2f;

        List<String> facts = new ArrayList<>();
        facts.add(typeLabel(maintenance.category));
        if (maintenance.odometerKm > 0) {
            facts.add(Formatters.km(maintenance.odometerKm));
        }
        if (!isEmpty(maintenance.workshop)) {
            facts.add(maintenance.workshop);
        }
        drawWrapped(join(facts, "  •  "), bodyMuted, MARGIN, CONTENT_WIDTH);

        for (MaintenanceItem item : entry.items) {
            ensureSpace(16f);
            String label = "– " + (isEmpty(item.name) ? typeLabel(item.type) : item.name);
            if (item.quantity > 1d) {
                label = label + "  x" + trimNumber(item.quantity);
            }
            String price = Formatters.money(item.totalPrice > 0d
                    ? item.totalPrice : item.unitPrice * item.quantity);
            float priceWidth = body.measureText(price);
            drawWrapped(label, body, MARGIN + 10f, CONTENT_WIDTH - 10f - priceWidth - 8f);
            canvas.drawText(price, MARGIN + CONTENT_WIDTH - priceWidth, cursorY - 13f, body);
        }

        if (!isEmpty(maintenance.notes)) {
            cursorY += 2f;
            drawWrapped("Obs.: " + maintenance.notes, bodyMuted, MARGIN + 10f, CONTENT_WIDTH - 10f);
        }

        cursorY += 8f;
        drawRule();
        cursorY += 10f;
    }

    private void drawPair(String label, String content) {
        ensureSpace(16f);
        canvas.drawText(label, MARGIN, cursorY, sectionLabel);
        canvas.drawText(content, MARGIN + 130f, cursorY, body);
        cursorY += 15f;
    }

    /** Escreve o texto quebrando por palavra e devolve o cursor logo abaixo. */
    private void drawWrapped(String content, Paint paint, float left, float maxWidth) {
        String[] words = content.trim().split("\\s+");
        StringBuilder line = new StringBuilder();
        for (String word : words) {
            String candidate = line.length() == 0 ? word : line + " " + word;
            if (paint.measureText(candidate) > maxWidth && line.length() > 0) {
                ensureSpace(14f);
                canvas.drawText(line.toString(), left, cursorY, paint);
                cursorY += 13f;
                line = new StringBuilder(word);
            } else {
                line = new StringBuilder(candidate);
            }
        }
        if (line.length() > 0) {
            ensureSpace(14f);
            canvas.drawText(line.toString(), left, cursorY, paint);
            cursorY += 13f;
        }
    }

    private void drawRule() {
        ensureSpace(8f);
        canvas.drawLine(MARGIN, cursorY, MARGIN + CONTENT_WIDTH, cursorY, rule);
        cursorY += 4f;
    }

    // ------------------------------------------------------------------ página

    private void ensureSpace(float needed) {
        if (cursorY + needed > BOTTOM_LIMIT) {
            finishPage();
            startPage();
            canvas.drawText(headerLine, MARGIN, cursorY, footer);
            cursorY += 16f;
        }
    }

    private void startPage() {
        pageNumber++;
        PdfDocument.PageInfo info =
                new PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create();
        page = document.startPage(info);
        canvas = page.getCanvas();
        cursorY = MARGIN;
    }

    private void finishPage() {
        if (page == null) {
            return;
        }
        String mark = "Página " + pageNumber;
        canvas.drawText(mark, MARGIN + CONTENT_WIDTH - footer.measureText(mark),
                PAGE_HEIGHT - MARGIN + 8f, footer);
        document.finishPage(page);
        page = null;
        canvas = null;
    }

    // ------------------------------------------------------------------ saída

    private static Uri write(Context context, PdfDocument document, String fileName)
            throws IOException {
        ContentResolver resolver = context.getContentResolver();
        ContentValues values = new ContentValues();
        values.put(MediaStore.Downloads.DISPLAY_NAME, fileName);
        values.put(MediaStore.Downloads.MIME_TYPE, "application/pdf");
        values.put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);
        values.put(MediaStore.Downloads.IS_PENDING, 1);

        Uri uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
        if (uri == null) {
            throw new IOException("Não foi possível criar o arquivo em Downloads");
        }
        try (OutputStream output = resolver.openOutputStream(uri)) {
            if (output == null) {
                throw new IOException("Downloads indisponível para escrita");
            }
            document.writeTo(output);
        } catch (IOException error) {
            resolver.delete(uri, null, null);
            throw error;
        }
        values.clear();
        values.put(MediaStore.Downloads.IS_PENDING, 0);
        resolver.update(uri, values, null, null);
        return uri;
    }

    private static String fileName(@Nullable Vehicle vehicle) {
        String base = vehicle == null ? "veiculo" : vehicle.displayName();
        String slug = slug(base);
        return "historico_manutencao_" + slug + "_"
                + new java.text.SimpleDateFormat("yyyyMMdd_HHmm", Locale.US)
                        .format(new java.util.Date()) + ".pdf";
    }

    private static String slug(String value) {
        String normalized = java.text.Normalizer.normalize(value, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.US)
                .replaceAll("[^a-z0-9]+", "_")
                .replaceAll("^_|_$", "");
        return normalized.isEmpty() ? "veiculo" : normalized;
    }

    // ------------------------------------------------------------------ helpers

    private static Paint text(float size, Typeface typeface, int color) {
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setTextSize(size);
        paint.setTypeface(typeface);
        paint.setColor(color);
        return paint;
    }

    private static String vehicleLine(@Nullable Vehicle vehicle) {
        if (vehicle == null) {
            return "Veículo não identificado";
        }
        // Com apelido, marca e modelo entram junto: o PDF é documento do carro,
        // não da forma como o dono o chama.
        String name = vehicle.hasNickname()
                ? vehicle.displayName() + " (" + vehicle.technicalName() + ")"
                : vehicle.technicalName();
        String line = name + " • " + vehicle.displaySpecs();
        return isEmpty(vehicle.plate) ? line : line + " • " + vehicle.plate;
    }

    private static String typeLabel(String enumName) {
        if (isEmpty(enumName)) {
            return MaintenanceType.OTHER.label();
        }
        try {
            return MaintenanceType.valueOf(enumName).label();
        } catch (IllegalArgumentException ignored) {
            return enumName;
        }
    }

    private static String trimNumber(double number) {
        if (number == Math.rint(number)) {
            return String.valueOf((long) number);
        }
        return String.format(Locale.US, "%.2f", number);
    }

    private static String join(List<String> parts, String separator) {
        StringBuilder builder = new StringBuilder();
        for (String part : parts) {
            if (isEmpty(part)) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append(separator);
            }
            builder.append(part);
        }
        return builder.toString();
    }

    private static boolean isEmpty(String value) {
        return value == null || value.trim().isEmpty();
    }
}
