package com.example.myapplication.ui.history;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.data.local.entity.MaintenanceItem;
import com.example.myapplication.data.local.relation.MaintenanceWithItems;
import com.example.myapplication.domain.model.MaintenanceType;
import com.example.myapplication.domain.model.UpcomingMaintenance;
import com.example.myapplication.ui.carbon.CarbonSectionTitle;
import com.example.myapplication.ui.carbon.CarbonStatusPalette;
import com.example.myapplication.util.DateUtils;
import com.example.myapplication.util.Formatters;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Lista técnica de manutenções: itens previstos pelo motor e manutenções já
 * registradas, na mesma linguagem visual.
 *
 * <p>Não calcula nada — recebe pronto o que o {@code VehicleViewModel} já expunha.
 */
public class MaintenanceListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    public interface Listener {
        void onCompletedClick(MaintenanceWithItems item);

        void onCompletedLongClick(MaintenanceWithItems item);

        void onPendingClick(UpcomingMaintenance item);
    }

    private static final int TYPE_SECTION = 0;
    private static final int TYPE_PENDING = 1;
    private static final int TYPE_COMPLETED = 2;

    private static class Row {
        int type;
        int index;
        int sectionRes;
        UpcomingMaintenance pending;
        MaintenanceWithItems completed;
    }

    private final List<Row> rows = new ArrayList<>();
    private final Listener listener;

    public MaintenanceListAdapter(Listener listener) {
        this.listener = listener;
    }

    /** @param withSections true quando as duas listas aparecem juntas (aba TODAS). */
    public void submit(List<UpcomingMaintenance> pending, List<MaintenanceWithItems> completed,
                       boolean withSections) {
        rows.clear();

        if (pending != null && !pending.isEmpty()) {
            if (withSections) {
                rows.add(section(R.string.carbon_section_pending));
            }
            int index = 1;
            for (UpcomingMaintenance item : pending) {
                Row row = new Row();
                row.type = TYPE_PENDING;
                row.pending = item;
                row.index = index++;
                rows.add(row);
            }
        }

        if (completed != null && !completed.isEmpty()) {
            if (withSections) {
                rows.add(section(R.string.carbon_section_completed));
            }
            int index = 1;
            for (MaintenanceWithItems item : completed) {
                Row row = new Row();
                row.type = TYPE_COMPLETED;
                row.completed = item;
                row.index = index++;
                rows.add(row);
            }
        }
        notifyDataSetChanged();
    }

    private Row section(int titleRes) {
        Row row = new Row();
        row.type = TYPE_SECTION;
        row.sectionRes = titleRes;
        return row;
    }

    @Override
    public int getItemViewType(int position) {
        return rows.get(position).type;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == TYPE_SECTION) {
            return new SectionViewHolder(
                    inflater.inflate(R.layout.item_carbon_section, parent, false));
        }
        if (viewType == TYPE_PENDING) {
            return new PendingViewHolder(
                    inflater.inflate(R.layout.item_maintenance_pending, parent, false));
        }
        return new CompletedViewHolder(inflater.inflate(R.layout.item_history, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Row row = rows.get(position);
        if (holder instanceof SectionViewHolder) {
            ((SectionViewHolder) holder).bind(row.sectionRes);
        } else if (holder instanceof PendingViewHolder) {
            ((PendingViewHolder) holder).bind(row.pending, row.index, listener);
        } else {
            ((CompletedViewHolder) holder).bind(row.completed, row.index, listener);
        }
    }

    @Override
    public int getItemCount() {
        return rows.size();
    }

    static class SectionViewHolder extends RecyclerView.ViewHolder {

        private final CarbonSectionTitle title;

        SectionViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.sectionTitle);
        }

        void bind(int titleRes) {
            title.setText(itemView.getContext().getString(titleRes));
        }
    }

    static class PendingViewHolder extends RecyclerView.ViewHolder {

        private final TextView textIndex;
        private final TextView textTitle;
        private final TextView textForecast;
        private final TextView textLast;
        private final TextView textStatus;

        PendingViewHolder(@NonNull View itemView) {
            super(itemView);
            textIndex = itemView.findViewById(R.id.textIndex);
            textTitle = itemView.findViewById(R.id.textTitle);
            textForecast = itemView.findViewById(R.id.textForecast);
            textLast = itemView.findViewById(R.id.textLast);
            textStatus = itemView.findViewById(R.id.textStatus);
        }

        void bind(final UpcomingMaintenance item, int index, final Listener listener) {
            textIndex.setText(String.format(Locale.US, "%02d", index));
            textTitle.setText(item.label);
            textForecast.setText(forecast(item));

            // Símbolo + texto: o estado não depende só da cor.
            textStatus.setText(itemView.getContext().getString(
                    CarbonStatusPalette.shortLabelRes(item.status)));
            textStatus.setTextColor(ContextCompat.getColor(itemView.getContext(),
                    CarbonStatusPalette.colorRes(item.status)));

            StringBuilder support = new StringBuilder(schedule(item));
            if (item.lastDoneDate > 0) {
                if (support.length() > 0) {
                    support.append(" • ");
                }
                support.append(itemView.getContext().getString(R.string.carbon_last_done,
                        DateUtils.formatShort(item.lastDoneDate)));
            }
            String last = support.toString();
            textLast.setText(last);
            textLast.setVisibility(last.isEmpty() ? View.GONE : View.VISIBLE);

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (listener != null) {
                        listener.onPendingClick(item);
                    }
                }
            });
        }

        /** Primeiro o que o usuário precisa saber: quanto falta. */
        private String forecast(UpcomingMaintenance item) {
            return item.remainingDetailed();
        }

        /** Detalhe de apoio: em que marca do hodômetro a troca acontece. */
        private String schedule(UpcomingMaintenance item) {
            if (item.nextDueKm <= 0) {
                return "";
            }
            return itemView.getContext().getString(R.string.carbon_forecast,
                    Formatters.km(item.nextDueKm));
        }
    }

    static class CompletedViewHolder extends RecyclerView.ViewHolder {

        private final TextView textIndex;
        private final TextView textTitle;
        private final TextView textDate;
        private final TextView textItems;
        private final TextView textStatus;
        private final TextView textValue;

        CompletedViewHolder(@NonNull View itemView) {
            super(itemView);
            textIndex = itemView.findViewById(R.id.textIndex);
            textTitle = itemView.findViewById(R.id.textTitle);
            textDate = itemView.findViewById(R.id.textDate);
            textItems = itemView.findViewById(R.id.textItems);
            textStatus = itemView.findViewById(R.id.textStatus);
            textValue = itemView.findViewById(R.id.textValue);
        }

        void bind(final MaintenanceWithItems entry, int index, final Listener listener) {
            textIndex.setText(String.format(Locale.US, "%02d", index));

            MaintenanceType type = MaintenanceType.fromName(entry.maintenance.category);
            String title = entry.maintenance.description;
            if (title == null || title.trim().isEmpty()) {
                title = type.label();
            }
            textTitle.setText(title);
            textDate.setText(DateUtils.formatHistory(entry.maintenance.date) + " • "
                    + Formatters.km(entry.maintenance.odometerKm));
            textStatus.setText(R.string.carbon_status_done);
            textValue.setText(Formatters.money(entry.maintenance.totalCost));

            StringBuilder parts = new StringBuilder();
            for (MaintenanceItem item : entry.items) {
                if (parts.length() > 0) {
                    parts.append(" • ");
                }
                parts.append(item.name);
            }
            textItems.setText(parts.toString());
            textItems.setVisibility(parts.length() == 0 ? View.GONE : View.VISIBLE);

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (listener != null) {
                        listener.onCompletedClick(entry);
                    }
                }
            });
            itemView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View view) {
                    if (listener == null) {
                        return false;
                    }
                    listener.onCompletedLongClick(entry);
                    return true;
                }
            });
        }
    }
}
