package com.example.myapplication.domain.document;

import android.net.Uri;

/**
 * Motor de OCR. O MVP usa {@link NoOpOcrEngine}; trocar por ML Kit / API remota
 * é só fornecer outra implementação ao {@link OcrDocumentAnalyzer}.
 */
public interface OcrEngine {

    boolean isAvailable();

    String extractText(Uri uri) throws AnalysisException;
}
