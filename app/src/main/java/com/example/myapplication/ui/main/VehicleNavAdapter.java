package com.example.myapplication.ui.main;

import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.data.local.entity.Vehicle;
import com.example.myapplication.util.AppExecutors;
import com.example.myapplication.util.Formatters;
import com.example.myapplication.util.ImageStore;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Garagem da barra lateral: um item por veículo, com marcação do ativo. */
public class VehicleNavAdapter extends RecyclerView.Adapter<VehicleNavAdapter.VehicleHolder> {

    public interface Listener {
        void onSelect(Vehicle vehicle);

        void onLongClick(Vehicle vehicle);

        /** Pedido de arrasto a partir da alça do item. */
        void onDragRequested(RecyclerView.ViewHolder holder);
    }

    private final List<Vehicle> items = new ArrayList<>();
    /** Fotos já decodificadas: a lista é curta e evita ler o disco a cada bind. */
    private final Map<String, Bitmap> photoCache = new HashMap<>();
    private final Listener listener;
    private long activeId;

    public VehicleNavAdapter(Listener listener) {
        this.listener = listener;
    }

    public void submit(List<Vehicle> vehicles, long activeId) {
        this.activeId = activeId;
        items.clear();
        if (vehicles != null) {
            items.addAll(vehicles);
        }
        notifyDataSetChanged();
    }

    /** Move o item na lista visível; a ordem só é gravada ao soltar. */
    public void move(int from, int to) {
        if (from < 0 || to < 0 || from >= items.size() || to >= items.size()) {
            return;
        }
        Collections.swap(items, from, to);
        notifyItemMoved(from, to);
    }

    /** Ids na ordem atual da tela, para persistir depois do arrasto. */
    public List<Long> currentOrder() {
        List<Long> ids = new ArrayList<>();
        for (Vehicle vehicle : items) {
            ids.add(vehicle.id);
        }
        return ids;
    }

    @NonNull
    @Override
    public VehicleHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_vehicle_nav, parent, false);
        return new VehicleHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull VehicleHolder holder, int position) {
        holder.bind(items.get(position));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    class VehicleHolder extends RecyclerView.ViewHolder {

        private final View viewActiveMark;
        private final ImageView imagePhoto;
        private final ImageView handleDrag;
        private final TextView textName;
        private final TextView textSpecs;
        private final TextView textKm;

        VehicleHolder(@NonNull View itemView) {
            super(itemView);
            viewActiveMark = itemView.findViewById(R.id.viewActiveMark);
            imagePhoto = itemView.findViewById(R.id.imagePhoto);
            handleDrag = itemView.findViewById(R.id.handleDrag);
            textName = itemView.findViewById(R.id.textName);
            textSpecs = itemView.findViewById(R.id.textSpecs);
            textKm = itemView.findViewById(R.id.textKm);
        }

        void bind(final Vehicle vehicle) {
            textName.setText(vehicle.displayName());

            // Com apelido, marca e modelo continuam visíveis embaixo: o apelido
            // identifica, mas não pode esconder qual carro é.
            StringBuilder specs = new StringBuilder();
            if (vehicle.hasNickname()) {
                specs.append(vehicle.technicalName()).append(" • ");
            }
            specs.append(vehicle.displaySpecs());
            if (vehicle.plate != null && !vehicle.plate.trim().isEmpty()) {
                specs.append(" • ").append(vehicle.plate.trim());
            }
            textSpecs.setText(specs.toString());
            textKm.setText(Formatters.km(vehicle.currentKm));

            viewActiveMark.setVisibility(vehicle.id == activeId ? View.VISIBLE : View.INVISIBLE);
            itemView.setSelected(vehicle.id == activeId);

            bindPhoto(vehicle);

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (listener != null) {
                        listener.onSelect(vehicle);
                    }
                }
            });
            itemView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View view) {
                    if (listener != null) {
                        listener.onLongClick(vehicle);
                    }
                    return true;
                }
            });
            handleDrag.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View view, MotionEvent event) {
                    if (event.getActionMasked() == MotionEvent.ACTION_DOWN && listener != null) {
                        listener.onDragRequested(VehicleHolder.this);
                    }
                    return false;
                }
            });
        }

        /** Foto é lida do disco fora da main thread; o ícone fica como fallback. */
        private void bindPhoto(final Vehicle vehicle) {
            final String path = vehicle.photoPath;
            imagePhoto.setTag(path);
            showPlaceholder();
            if (path == null || path.isEmpty()) {
                return;
            }
            Bitmap cached = photoCache.get(path);
            if (cached != null) {
                showPhoto(cached);
                return;
            }
            AppExecutors.get().diskIO().execute(new Runnable() {
                @Override
                public void run() {
                    final Bitmap bitmap = ImageStore.load(path);
                    if (bitmap == null) {
                        return;
                    }
                    AppExecutors.get().mainThread(new Runnable() {
                        @Override
                        public void run() {
                            photoCache.put(path, bitmap);
                            if (path.equals(imagePhoto.getTag())) {
                                showPhoto(bitmap);
                            }
                        }
                    });
                }
            });
        }

        private void showPhoto(Bitmap bitmap) {
            imagePhoto.setPadding(0, 0, 0, 0);
            imagePhoto.setImageTintList(null);
            imagePhoto.setImageBitmap(bitmap);
        }

        private void showPlaceholder() {
            int padding = itemView.getResources()
                    .getDimensionPixelSize(R.dimen.carbon_space_sm);
            imagePhoto.setPadding(padding, padding, padding, padding);
            imagePhoto.setImageResource(R.drawable.ic_car);
            // A foto zera o tint; o ícone de fallback precisa dele de volta.
            imagePhoto.setImageTintList(ColorStateList.valueOf(ContextCompat.getColor(
                    itemView.getContext(), R.color.carbon_text_dim)));
        }
    }
}
