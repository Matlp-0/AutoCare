package com.example.myapplication.domain.document;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.pdf.PdfRenderer;
import android.net.Uri;
import android.os.ParcelFileDescriptor;

import com.google.android.gms.tasks.Tasks;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

/**
 * OCR local com ML Kit (modelo latino embarcado, sem chave de API e sem enviar o
 * documento para servidor nenhum).
 *
 * <p>Fotos vão direto para o reconhecedor. PDFs são renderizados página a página
 * com o {@link PdfRenderer} do Android e cada página passa pelo mesmo OCR — assim
 * funciona tanto para PDF "de texto" quanto para PDF que é imagem escaneada.
 *
 * <p>Sempre chamado a partir de uma thread de trabalho: as chamadas do ML Kit são
 * aguardadas de forma síncrona.
 */
public class MlKitOcrEngine implements OcrEngine {

    /** Páginas além disso raramente trazem itens da nota e custam memória. */
    private static final int MAX_PDF_PAGES = 4;
    /** Escala de renderização do PDF: texto pequeno precisa de mais pixels. */
    private static final int PDF_SCALE = 2;
    private static final int MAX_BITMAP_SIDE = 3000;

    private final Context context;

    public MlKitOcrEngine(Context context) {
        this.context = context.getApplicationContext();
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public String extractText(Uri uri) throws AnalysisException {
        if (uri == null) {
            throw new AnalysisException("Documento inválido.");
        }
        String type = context.getContentResolver().getType(uri);
        boolean isPdf = (type != null && type.contains("pdf"))
                || uri.toString().toLowerCase().endsWith(".pdf");
        return isPdf ? extractFromPdf(uri) : extractFromImage(uri);
    }

    private String extractFromImage(Uri uri) throws AnalysisException {
        TextRecognizer recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
        try {
            InputImage image = InputImage.fromFilePath(context, uri);
            return recognize(recognizer, image);
        } catch (IOException error) {
            throw new AnalysisException("Não foi possível abrir a imagem.", error);
        } finally {
            recognizer.close();
        }
    }

    private String extractFromPdf(Uri uri) throws AnalysisException {
        TextRecognizer recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
        StringBuilder text = new StringBuilder();
        ParcelFileDescriptor descriptor = null;
        PdfRenderer renderer = null;
        try {
            descriptor = context.getContentResolver().openFileDescriptor(uri, "r");
            if (descriptor == null) {
                throw new AnalysisException("Não foi possível abrir o PDF.");
            }
            renderer = new PdfRenderer(descriptor);
            int pages = Math.min(renderer.getPageCount(), MAX_PDF_PAGES);
            for (int index = 0; index < pages; index++) {
                Bitmap bitmap = renderPage(renderer, index);
                try {
                    String pageText = recognize(recognizer, InputImage.fromBitmap(bitmap, 0));
                    if (!pageText.isEmpty()) {
                        text.append(pageText).append('\n');
                    }
                } finally {
                    bitmap.recycle();
                }
            }
        } catch (IOException error) {
            throw new AnalysisException("PDF inválido ou protegido: " + error.getMessage(), error);
        } catch (SecurityException error) {
            throw new AnalysisException("Sem permissão para ler este arquivo.", error);
        } finally {
            recognizer.close();
            closeQuietly(renderer, descriptor);
        }
        return text.toString();
    }

    private Bitmap renderPage(PdfRenderer renderer, int index) {
        PdfRenderer.Page page = renderer.openPage(index);
        try {
            int width = Math.min(page.getWidth() * PDF_SCALE, MAX_BITMAP_SIDE);
            int height = Math.min(page.getHeight() * PDF_SCALE, MAX_BITMAP_SIDE);
            Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            // O PdfRenderer não pinta o fundo: sem o branco, o texto some no OCR.
            new Canvas(bitmap).drawColor(Color.WHITE);
            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
            return bitmap;
        } finally {
            page.close();
        }
    }

    private String recognize(TextRecognizer recognizer, InputImage image)
            throws AnalysisException {
        try {
            return Tasks.await(recognizer.process(image)).getText();
        } catch (ExecutionException | InterruptedException error) {
            throw new AnalysisException("Falha ao ler o texto do documento.", error);
        }
    }

    private void closeQuietly(PdfRenderer renderer, ParcelFileDescriptor descriptor) {
        if (renderer != null) {
            try {
                renderer.close();
            } catch (RuntimeException ignored) {
                // renderer já fechado
            }
        }
        if (descriptor != null) {
            try {
                descriptor.close();
            } catch (IOException ignored) {
                // nada a fazer
            }
        }
    }
}
