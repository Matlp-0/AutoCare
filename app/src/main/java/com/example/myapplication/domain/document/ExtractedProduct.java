package com.example.myapplication.domain.document;

import com.example.myapplication.domain.model.MaintenanceType;

import java.io.Serializable;

/** Produto/serviço encontrado em uma nota fiscal. */
public class ExtractedProduct implements Serializable {

    public String name;
    public double quantity = 1d;
    public double unitPrice;
    public double totalPrice;
    public MaintenanceType suggestedType = MaintenanceType.OTHER;
    /** O usuário pode desmarcar itens antes de confirmar. */
    public boolean selected = true;

    public ExtractedProduct() {
    }

    public ExtractedProduct(String name, double quantity, double unitPrice, double totalPrice) {
        this.name = name;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.totalPrice = totalPrice;
    }
}
