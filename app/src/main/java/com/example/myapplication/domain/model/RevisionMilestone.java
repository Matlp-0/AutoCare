package com.example.myapplication.domain.model;

import java.util.ArrayList;
import java.util.List;

/** Marco da timeline da tela "Revisões" (ex.: revisão de 40.000 km). */
public class RevisionMilestone {

    public enum State {
        DONE("Realizada", "✓"),
        NEXT("Próxima", "⚠"),
        FUTURE("Futuramente", "○"),
        LATE("Atrasada", "!"),
        /** Marco antigo sem registro no histórico — pode ter sido feito antes do app. */
        NO_RECORD("Sem registro", "–");

        private final String label;
        private final String icon;

        State(String label, String icon) {
            this.label = label;
            this.icon = icon;
        }

        public String label() {
            return label;
        }

        public String icon() {
            return icon;
        }
    }

    public int km;
    public State state = State.FUTURE;
    /** true quando o estado veio de uma marcação explícita do usuário. */
    public boolean userMarked;
    public List<MaintenanceType> items = new ArrayList<>();
    /** Nomes dos itens como aparecem no plano do fabricante. */
    public List<String> labels = new ArrayList<>();
    public String description;

    public String itemsText() {
        StringBuilder builder = new StringBuilder();
        if (!labels.isEmpty()) {
            for (String label : labels) {
                if (builder.length() > 0) {
                    builder.append(" • ");
                }
                builder.append(label);
            }
            return builder.toString();
        }
        for (MaintenanceType type : items) {
            if (builder.length() > 0) {
                builder.append(" • ");
            }
            builder.append(type.label());
        }
        return builder.toString();
    }
}
