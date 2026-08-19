package com.example.myapplication.domain.document;

import com.example.myapplication.domain.model.MaintenanceType;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/** Resultado da leitura de um documento (XML de NFe, PDF ou foto). */
public class ExtractedInvoice implements Serializable {

    public String documentType;
    public String uri;
    public String invoiceNumber;
    public String cnpj;
    public String companyName;
    public long issueDate;
    public double totalValue;
    public String rawText;
    public List<ExtractedProduct> products = new ArrayList<>();

    /** Sugestão calculada pelo MaintenanceInterpreter. */
    public MaintenanceType suggestedType = MaintenanceType.OTHER;
    public String suggestedDescription;
    /** true quando a extração é parcial e depende de revisão do usuário. */
    public boolean needsReview = true;
}
