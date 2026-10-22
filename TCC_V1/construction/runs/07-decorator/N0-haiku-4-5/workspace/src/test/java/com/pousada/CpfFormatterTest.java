package com.pousada;

import com.pousada.utils.CpfFormatter;
import org.junit.Test;
import static org.junit.Assert.*;

public class CpfFormatterTest {
    @Test
    public void formatCpf() {
        assertEquals("111.222.333-44", CpfFormatter.format("11122233344"));
    }

    @Test
    public void unformatCpf() {
        assertEquals("11122233344", CpfFormatter.unformat("111.222.333-44"));
    }

    @Test
    public void roundTrip() {
        String original = "11122233344";
        String formatted = CpfFormatter.format(original);
        String unformatted = CpfFormatter.unformat(formatted);
        assertEquals(original, unformatted);
    }
}
