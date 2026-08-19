package com.example.myapplication;

import static org.junit.Assert.assertEquals;

import com.example.myapplication.domain.document.ExtractedInvoice;
import com.example.myapplication.domain.document.ExtractedProduct;
import com.example.myapplication.domain.document.MaintenanceInterpreter;
import com.example.myapplication.domain.model.MaintenanceType;

import org.junit.Test;

public class MaintenanceInterpreterTest {

    private final MaintenanceInterpreter interpreter = new MaintenanceInterpreter();

    @Test
    public void classifiesOilByProductName() {
        assertEquals(MaintenanceType.OIL_CHANGE,
                interpreter.classify("Óleo Lubrificante 5W30"));
        assertEquals(MaintenanceType.OIL_FILTER,
                interpreter.classify("FILTRO DE OLEO TECFIL"));
        assertEquals(MaintenanceType.BRAKE_PADS,
                interpreter.classify("Pastilha de freio dianteira"));
        assertEquals(MaintenanceType.OTHER,
                interpreter.classify("Chaveiro personalizado"));
    }

    @Test
    public void suggestsMaintenanceFromInvoice() {
        ExtractedInvoice invoice = new ExtractedInvoice();
        invoice.products.add(new ExtractedProduct("Óleo Lubrificante 5W30", 4, 45d, 180d));
        invoice.products.add(new ExtractedProduct("Filtro de óleo", 1, 60d, 60d));

        interpreter.interpret(invoice);

        assertEquals(MaintenanceType.OIL_CHANGE, invoice.suggestedType);
        assertEquals("Troca de óleo + Filtro de óleo", invoice.suggestedDescription);
    }
}
