package com.example.myapplication.domain.document;

import android.content.Context;

import com.example.myapplication.data.local.entity.Document;

/** Escolhe o analisador conforme o tipo de documento importado. */
public class DocumentAnalyzerFactory {

    private final Context context;
    private final MaintenanceInterpreter interpreter;
    private final OcrEngine ocrEngine;

    public DocumentAnalyzerFactory(Context context, MaintenanceInterpreter interpreter,
                                   OcrEngine ocrEngine) {
        this.context = context.getApplicationContext();
        this.interpreter = interpreter;
        this.ocrEngine = ocrEngine;
    }

    public DocumentAnalyzer create(String documentType) {
        if (Document.TYPE_XML.equals(documentType)) {
            return new NfeXmlDocumentAnalyzer(context, interpreter);
        }
        return new OcrDocumentAnalyzer(ocrEngine, interpreter, documentType);
    }

    public boolean isOcrAvailable() {
        return ocrEngine.isAvailable();
    }
}
