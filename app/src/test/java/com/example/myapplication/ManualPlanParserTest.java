package com.example.myapplication;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.example.myapplication.domain.document.MaintenanceInterpreter;
import com.example.myapplication.domain.manual.ManualPlanEntry;
import com.example.myapplication.domain.manual.ManualPlanItem;
import com.example.myapplication.domain.manual.ManualPlanParser;
import com.example.myapplication.domain.model.MaintenanceType;

import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

/** Usa uma tabela real de plano de revisão salva como fixture. */
public class ManualPlanParserTest {

    private final ManualPlanParser parser = new ManualPlanParser(new MaintenanceInterpreter());

    @Test
    public void parsesRealRevisionTable() throws Exception {
        List<ManualPlanEntry> plan = parser.parse(fixture("plano-revisao-gol.html"));

        assertTrue("deveria encontrar várias revisões", plan.size() >= 4);
        assertEquals(10000, plan.get(0).intervalKm);
        assertEquals(20000, plan.get(1).intervalKm);
        assertEquals(12, plan.get(0).intervalMonths);

        ManualPlanEntry first = plan.get(0);
        assertTrue("revisão deve listar itens", first.planItems.size() >= 3);
        assertTrue("óleo deve estar na revisão de 10.000 km",
                first.items.contains(MaintenanceType.OIL_CHANGE));

        // O nome original do manual é preservado, não só o tipo interno.
        ManualPlanItem oil = findByType(first, MaintenanceType.OIL_CHANGE);
        assertNotNull(oil);
        assertTrue(oil.label.toLowerCase().contains("óleo"));
    }

    @Test
    public void returnsEmptyForPageWithoutTable() {
        assertTrue(parser.parse("<html><body><p>sem tabela aqui</p></body></html>").isEmpty());
        assertTrue(parser.parse("").isEmpty());
        assertTrue(parser.parse(null).isEmpty());
    }

    @Test
    public void ignoresColumnsWithoutMark() {
        String html = "<table>"
                + "<tr><th>Modelo</th><th>10 mil km</th><th>20 mil km</th></tr>"
                + "<tr><td>Filtro de Ar do Motor</td><td>-</td><td>&#10004;</td></tr>"
                + "<tr><td>Óleo de Motor</td><td>&#10004;</td><td>&#10004;</td></tr>"
                + "</table>";

        List<ManualPlanEntry> plan = parser.parse(html);

        assertEquals(2, plan.size());
        assertEquals(1, plan.get(0).planItems.size());
        assertEquals(MaintenanceType.OIL_CHANGE, plan.get(0).items.get(0));
        assertEquals(2, plan.get(1).planItems.size());
    }

    private ManualPlanItem findByType(ManualPlanEntry entry, MaintenanceType type) {
        for (ManualPlanItem item : entry.planItems) {
            if (item.type == type) {
                return item;
            }
        }
        return null;
    }

    private String fixture(String name) throws Exception {
        InputStream stream = getClass().getClassLoader().getResourceAsStream(name);
        assertNotNull("fixture não encontrada: " + name, stream);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int read;
        while ((read = stream.read(buffer)) != -1) {
            out.write(buffer, 0, read);
        }
        stream.close();
        return new String(out.toByteArray(), StandardCharsets.UTF_8);
    }
}
