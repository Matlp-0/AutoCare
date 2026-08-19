package com.example.myapplication.ui.home;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.domain.model.UpcomingMaintenance;
import com.example.myapplication.ui.carbon.CarbonStatusPalette;
import com.example.myapplication.util.DateUtils;
import com.example.myapplication.util.Formatters;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Lista "Próximas manutenções" no formato de linha do Carbon UI. */
public class UpcomingAdapter extends RecyclerView.Adapter<UpcomingAdapter.UpcomingViewHolder> {

    private final List<UpcomingMaintenance> items = new ArrayList<>();

    public void submit(List<UpcomingMaintenance> newItems) {
        items.clear();
        if (newItems != null) {
            items.addAll(newItems);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public UpcomingViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_upcoming, parent, false);
        return new UpcomingViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull UpcomingViewHolder holder, int position) {
        holder.bind(items.get(position), position + 1);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class UpcomingViewHolder extends RecyclerView.ViewHolder {

        private final TextView textIndex;
        private final TextView textTitle;
        private final TextView textRemaining;
        private final TextView textSubtitle;
        private final TextView textStatus;

        UpcomingViewHolder(@NonNull View itemView) {
            super(itemView);
            textIndex = itemView.findViewById(R.id.textIndex);
            textTitle = itemView.findViewById(R.id.textTitle);
            textRemaining = itemView.findViewById(R.id.textRemaining);
            textSubtitle = itemView.findViewById(R.id.textSubtitle);
            textStatus = itemView.findViewById(R.id.textStatus);
        }

        void bind(UpcomingMaintenance item, int position) {
            textIndex.setText(String.format(Locale.US, "%02d", position));
            textTitle.setText(item.label);
            textRemaining.setText(item.remainingDetailed());

            textStatus.setText(CarbonStatusPalette.shortLabelRes(item.status));
            textStatus.setTextColor(ContextCompat.getColor(itemView.getContext(),
                    CarbonStatusPalette.colorRes(item.status)));

            String detail = detail(item);
            textSubtitle.setText(detail);
            textSubtitle.setVisibility(detail.isEmpty() ? View.GONE : View.VISIBLE);
        }

        /** Mesma informação secundária que a Home já exibia antes da reformulação. */
        private String detail(UpcomingMaintenance item) {
            StringBuilder builder = new StringBuilder();
            if (item.nextDueKm > 0) {
                builder.append(itemView.getContext().getString(R.string.carbon_forecast,
                        Formatters.km(item.nextDueKm)));
            }
            if (item.lastDoneDate > 0) {
                if (builder.length() > 0) {
                    builder.append(" • ");
                }
                builder.append(itemView.getContext().getString(R.string.carbon_last_done,
                        DateUtils.formatShort(item.lastDoneDate)));
            }
            return builder.toString();
        }
    }
}
