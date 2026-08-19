package com.example.myapplication.domain.manual;

import java.util.ArrayList;
import java.util.List;

/** Resultado da identificação do veículo (manual do proprietário + plano). */
public class ManualInfo {

    public static final String SOURCE_REMOTE = "Consulta online";

    public String title;
    public String source = SOURCE_REMOTE;
    /** Preenchido quando existir consulta remota do manual. */
    public String manualUrl;
    public boolean exactMatch;
    public List<ManualPlanEntry> plan = new ArrayList<>();

    public ManualInfo(String title) {
        this.title = title;
    }
}
