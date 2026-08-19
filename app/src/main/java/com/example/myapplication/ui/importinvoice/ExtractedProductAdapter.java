package com.example.myapplication.ui.importinvoice;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.domain.document.ExtractedProduct;
import com.example.myapplication.domain.model.MaintenanceType;
import com.example.myapplication.util.Formatters;
import com.google.android.material.checkbox.MaterialCheckBox;

import java.util.ArrayList;
import java.util.List;

public class ExtractedProductAdapter
        extends RecyclerView.Adapter<ExtractedProductAdapter.ProductViewHolder> {

    private final List<ExtractedProduct> products = new ArrayList<>();

    public void submit(List<ExtractedProduct> newProducts) {
        products.clear();
        if (newProducts != null) {
            products.addAll(newProducts);
        }
        notifyDataSetChanged();
    }

    public List<ExtractedProduct> selected() {
        List<ExtractedProduct> selected = new ArrayList<>();
        for (ExtractedProduct product : products) {
            if (product.selected) {
                selected.add(product);
            }
        }
        return selected;
    }

    @NonNull
    @Override
    public ProductViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_extracted_product, parent, false);
        return new ProductViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ProductViewHolder holder, int position) {
        holder.bind(products.get(position));
    }

    @Override
    public int getItemCount() {
        return products.size();
    }

    static class ProductViewHolder extends RecyclerView.ViewHolder {

        private final MaterialCheckBox check;
        private final TextView textName;
        private final TextView textSuggestion;
        private final TextView textPrice;

        ProductViewHolder(@NonNull View itemView) {
            super(itemView);
            check = itemView.findViewById(R.id.checkProduct);
            textName = itemView.findViewById(R.id.textName);
            textSuggestion = itemView.findViewById(R.id.textSuggestion);
            textPrice = itemView.findViewById(R.id.textPrice);
        }

        void bind(final ExtractedProduct product) {
            textName.setText(product.name);
            MaintenanceType type = product.suggestedType;
            textSuggestion.setText(type == MaintenanceType.OTHER
                    ? "Sem sugestão automática"
                    : "Sugestão: " + type.label());
            textPrice.setText(Formatters.money(product.totalPrice));
            check.setOnCheckedChangeListener(null);
            check.setChecked(product.selected);
            check.setOnCheckedChangeListener((button, checked) -> product.selected = checked);
        }
    }
}
