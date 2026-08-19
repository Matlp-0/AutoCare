package com.example.myapplication.domain.document;

import android.net.Uri;

/**
 * Contrato de leitura de documentos. Hoje existem duas implementações locais
 * (XML de NFe e OCR stub). Uma implementação baseada em IA pode ser plugada
 * depois sem mudar as telas.
 */
public interface DocumentAnalyzer {

    ExtractedInvoice analyze(Uri uri) throws AnalysisException;
}
