package br.com.pousada.util;

import org.junit.jupiter.api.Test;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

public class DateUtilTest {

    @Test
    public void testHighSeason() {
        // Julho inteiro
        assertTrue(DateUtil.isHighSeason(LocalDate.of(2025, 7, 1)));
        assertTrue(DateUtil.isHighSeason(LocalDate.of(2025, 7, 31)));

        // 15 de dezembro em diante
        assertTrue(DateUtil.isHighSeason(LocalDate.of(2025, 12, 15)));
        assertTrue(DateUtil.isHighSeason(LocalDate.of(2025, 12, 31)));

        // Fevereiro
        assertTrue(DateUtil.isHighSeason(LocalDate.of(2026, 2, 1)));
        assertTrue(DateUtil.isHighSeason(LocalDate.of(2026, 2, 28)));

        // Fora da alta temporada
        assertFalse(DateUtil.isHighSeason(LocalDate.of(2025, 3, 1)));
        assertFalse(DateUtil.isHighSeason(LocalDate.of(2025, 10, 1)));
        assertFalse(DateUtil.isHighSeason(LocalDate.of(2025, 12, 14)));
    }

    @Test
    public void testDateFormatting() {
        LocalDate date = LocalDate.of(2027, 7, 10);

        String isoFormat = DateUtil.formatIsoDate(date);
        assertEquals("2027-07-10", isoFormat);

        String brFormat = DateUtil.formatBrDate(date);
        assertEquals("10/07/2027", brFormat);
    }
}
