package com.example.myapplication;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.net.Uri;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.example.myapplication.domain.document.ExtractedInvoice;
import com.example.myapplication.domain.document.MaintenanceInterpreter;
import com.example.myapplication.domain.document.NfeXmlDocumentAnalyzer;
import com.example.myapplication.domain.model.MaintenanceType;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;

@RunWith(AndroidJUnit4.class)
public class NfeXmlDocumentAnalyzerTest {

    private static final String NFE = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
            + "<nfeProc xmlns=\"http://www.portalfiscal.inf.br/nfe\"><NFe><infNFe>"
            + "<ide><nNF>12345</nNF><dhEmi>2026-08-10T10:15:00-03:00</dhEmi></ide>"
            + "<emit><CNPJ>12345678000199</CNPJ><xNome>AUTO CENTER X LTDA</xNome></emit>"
            + "<dest><CNPJ>99999999000199</CNPJ><xNome>CLIENTE TESTE</xNome></dest>"
            + "<det nItem=\"1\"><prod><xProd>OLEO LUBRIFICANTE 5W30</xProd>"
            + "<qCom>4.0000</qCom><vUnCom>45.0000</vUnCom><vProd>180.00</vProd></prod></det>"
            + "<det nItem=\"2\"><prod><xProd>FILTRO DE OLEO</xProd>"
            + "<qCom>1.0000</qCom><vUnCom>60.0000</vUnCom><vProd>60.00</vProd></prod></det>"
            + "<total><ICMSTot><vNF>240.00</vNF></ICMSTot></total>"
            + "</infNFe></NFe></nfeProc>";

    @Test
    public void parsesNfeAndSuggestsMaintenance() throws Exception {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        File file = new File(context.getCacheDir(), "nfe_test.xml");
        try (OutputStreamWriter writer =
                     new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8)) {
            writer.write(NFE);
        }

        NfeXmlDocumentAnalyzer analyzer =
                new NfeXmlDocumentAnalyzer(context, new MaintenanceInterpreter());
        ExtractedInvoice invoice = analyzer.analyze(Uri.fromFile(file));

        assertEquals("12345", invoice.invoiceNumber);
        assertEquals("12345678000199", invoice.cnpj);
        assertEquals("AUTO CENTER X LTDA", invoice.companyName);
        assertEquals(240.00, invoice.totalValue, 0.001);
        assertEquals(2, invoice.products.size());
        assertEquals("OLEO LUBRIFICANTE 5W30", invoice.products.get(0).name);
        assertEquals(180.00, invoice.products.get(0).totalPrice, 0.001);
        assertEquals(MaintenanceType.OIL_CHANGE, invoice.products.get(0).suggestedType);
        assertEquals(MaintenanceType.OIL_FILTER, invoice.products.get(1).suggestedType);
        assertEquals(MaintenanceType.OIL_CHANGE, invoice.suggestedType);
        assertTrue(invoice.issueDate > 0);
    }
}
