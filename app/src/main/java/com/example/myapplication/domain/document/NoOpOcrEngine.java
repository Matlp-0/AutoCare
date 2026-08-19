package com.example.myapplication.domain.document;

import android.net.Uri;

/** OCR ainda não integrado: o documento é anexado e o usuário completa os dados. */
public class NoOpOcrEngine implements OcrEngine {

    @Override
    public boolean isAvailable() {
        return false;
    }

    @Override
    public String extractText(Uri uri) {
        return "";
    }
}
