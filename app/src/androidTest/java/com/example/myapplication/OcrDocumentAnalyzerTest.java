package com.example.myapplication;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.example.myapplication.data.local.entity.Document;
import com.example.myapplication.domain.document.ExtractedInvoice;
import com.example.myapplication.domain.document.ExtractedProduct;
import com.example.myapplication.domain.document.MaintenanceInterpreter;
import com.example.myapplication.domain.document.MlKitOcrEngine;
import com.example.myapplication.domain.document.OcrDocumentAnalyzer;
import com.example.myapplication.domain.model.MaintenanceType;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.FileOutputStream;

/**
 * OCR de verdade: gera uma "nota" (imagem e PDF), passa pelo ML Kit e confere
 * os campos que a tela de confirmação vai mostrar.
 */
@RunWith(AndroidJUnit4.class)
public class OcrDocumentAnalyzerTest {

    private static final String[] NOTA = {
            "AUTO CENTER SAO PAULO LTDA",
            "CNPJ: 12.345.678/0001-99",
            "NOTA FISCAL No 004521",
            "DATA: 15/08/2026",
            "OLEO LUBRIFICANTE 5W30 200,00",
            "FILTRO DE OLEO 60,00",
            "PASTILHA DE FREIO 180,00",
            "VALOR TOTAL 440,00"
    };

    @Test
    public void readsInvoiceFromPhoto() throws Exception {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        File file = new File(context.getCacheDir(), "nota_ocr.png");
        writeImage(file);

        ExtractedInvoice invoice = analyze(context, Uri.fromFile(file), Document.TYPE_PHOTO);

        assertNotNull(invoice.rawText);
        assertTrue("OCR não leu nada", invoice.rawText.length() > 20);
        assertEquals(440.00, invoice.totalValue, 0.01);
        assertEquals("12.345.678/0001-99", invoice.cnpj);
        assertEquals("004521", invoice.invoiceNumber);
        assertTrue("deveria identificar itens", invoice.products.size() >= 2);
        assertTrue(hasType(invoice, MaintenanceType.OIL_CHANGE));
        assertTrue(hasType(invoice, MaintenanceType.OIL_FILTER));
    }

    @Test
    public void readsInvoiceFromPdf() throws Exception {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        File file = new File(context.getCacheDir(), "nota_ocr.pdf");
        writePdf(file);

        ExtractedInvoice invoice = analyze(context, Uri.fromFile(file), Document.TYPE_PDF);

        assertTrue("OCR não leu o PDF", invoice.rawText.length() > 20);
        assertEquals(440.00, invoice.totalValue, 0.01);
        assertTrue(hasType(invoice, MaintenanceType.OIL_CHANGE));
    }

    private ExtractedInvoice analyze(Context context, Uri uri, String type) throws Exception {
        OcrDocumentAnalyzer analyzer = new OcrDocumentAnalyzer(
                new MlKitOcrEngine(context), new MaintenanceInterpreter(), type);
        return analyzer.analyze(uri);
    }

    private boolean hasType(ExtractedInvoice invoice, MaintenanceType type) {
        for (ExtractedProduct product : invoice.products) {
            if (product.suggestedType == type) {
                return true;
            }
        }
        return false;
    }

    private void writeImage(File file) throws Exception {
        Bitmap bitmap = Bitmap.createBitmap(1000, 700, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        drawNota(canvas);
        try (FileOutputStream out = new FileOutputStream(file)) {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
        }
        bitmap.recycle();
    }

    private void writePdf(File file) throws Exception {
        PdfDocument document = new PdfDocument();
        PdfDocument.Page page = document.startPage(
                new PdfDocument.PageInfo.Builder(1000, 700, 1).create());
        drawNota(page.getCanvas());
        document.finishPage(page);
        try (FileOutputStream out = new FileOutputStream(file)) {
            document.writeTo(out);
        }
        document.close();
    }

    private void drawNota(Canvas canvas) {
        canvas.drawColor(Color.WHITE);
        Paint paint = new Paint();
        paint.setColor(Color.BLACK);
        paint.setTextSize(34f);
        paint.setAntiAlias(true);
        int y = 70;
        for (String line : NOTA) {
            canvas.drawText(line, 40, y, paint);
            y += 60;
        }
    }
}
