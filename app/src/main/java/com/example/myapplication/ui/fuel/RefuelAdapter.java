package com.example.myapplication.ui.fuel;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.data.local.entity.Refuel;
import com.example.myapplication.domain.model.FuelType;
import com.example.myapplication.util.DateUtils;
import com.example.myapplication.util.Formatters;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Extrato de abastecimentos. O consumo do trecho vem calculado do domínio. */
public class RefuelAdapter extends RecyclerView.Adapter<RefuelAdapter.RefuelHolder> {

    public interface Listener {
        void onClick(Refuel refuel);

        void onLongClick(Refuel refuel);
    }

    private final List<Refuel> items = new ArrayList<>();
    private final Map<Long, Double> consumptionByRefuel = new HashMap<>();
    private final Listener listener;

    public RefuelAdapter(Listener listener) {
        this.listener = listener;
    }

    public void submit(List<Refuel> refuels, Map<Long, Double> consumption) {
        items.clear();
        consumptionByRefuel.clear();
        if (refuels != null) {
            items.addAll(refuels);
        }
        if (consumption != null) {
            consumptionByRefuel.putAll(consumption);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public RefuelHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_refuel, parent, false);
        return new RefuelHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RefuelHolder holder, int position) {
        holder.bind(items.get(position));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    class RefuelHolder extends RecyclerView.ViewHolder {

        private final TextView textDate;
        private final TextView textDetails;
        private final TextView textStation;
        private final TextView textConsumption;
        private final TextView textTotal;

        RefuelHolder(@NonNull View itemView) {
            super(itemView);
            textDate = itemView.findViewById(R.id.textDate);
            textDetails = itemView.findViewById(R.id.textDetails);
            textStation = itemView.findViewById(R.id.textStation);
            textConsumption = itemView.findViewById(R.id.textConsumption);
            textTotal = itemView.findViewById(R.id.textTotal);
        }

        void bind(final Refuel refuel) {
            textDate.setText(DateUtils.formatHistory(refuel.date));
            textDetails.setText(itemView.getContext().getString(R.string.fuel_row_details,
                    Formatters.liters(refuel.liters),
                    Formatters.pricePerLiter(refuel.pricePerLiter),
                    Formatters.km(refuel.odometerKm)));

            StringBuilder caption = new StringBuilder(FuelType.fromName(refuel.fuelType).label());
            if (refuel.station != null && !refuel.station.trim().isEmpty()) {
                caption.append(" • ").append(refuel.station.trim());
            }
            if (!refuel.fullTank) {
                caption.append(" • ")
                        .append(itemView.getContext().getString(R.string.fuel_partial_tag));
            }
            textStation.setText(caption.toString());

            Double consumption = consumptionByRefuel.get(refuel.id);
            textConsumption.setText(consumption == null ? ""
                    : Formatters.consumption(consumption));
            textTotal.setText(Formatters.money(refuel.totalCost > 0d
                    ? refuel.totalCost : refuel.liters * refuel.pricePerLiter));

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (listener != null) {
                        listener.onClick(refuel);
                    }
                }
            });
            itemView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View view) {
                    if (listener != null) {
                        listener.onLongClick(refuel);
                    }
                    return true;
                }
            });
        }
    }
}
