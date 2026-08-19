package com.example.myapplication.ui.onboarding;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.domain.manual.ManualPlanEntry;
import com.example.myapplication.domain.model.MaintenanceType;
import com.example.myapplication.util.Formatters;

import java.util.ArrayList;
import java.util.List;

public class PlanAdapter extends RecyclerView.Adapter<PlanAdapter.PlanViewHolder> {

    private final List<ManualPlanEntry> entries = new ArrayList<>();

    public void submit(List<ManualPlanEntry> newEntries) {
        entries.clear();
        if (newEntries != null) {
            entries.addAll(newEntries);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public PlanViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_plan_entry, parent, false);
        return new PlanViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PlanViewHolder holder, int position) {
        holder.bind(entries.get(position));
    }

    @Override
    public int getItemCount() {
        return entries.size();
    }

    static class PlanViewHolder extends RecyclerView.ViewHolder {

        private final TextView textInterval;
        private final TextView textItems;

        PlanViewHolder(@NonNull View itemView) {
            super(itemView);
            textInterval = itemView.findViewById(R.id.textInterval);
            textItems = itemView.findViewById(R.id.textItems);
        }

        void bind(ManualPlanEntry entry) {
            textInterval.setText(Formatters.km(entry.intervalKm));
            StringBuilder builder = new StringBuilder();
            for (String label : entry.labels()) {
                if (builder.length() > 0) {
                    builder.append('\n');
                }
                builder.append("• ").append(label);
            }
            textItems.setText(builder.toString());
        }
    }
}
