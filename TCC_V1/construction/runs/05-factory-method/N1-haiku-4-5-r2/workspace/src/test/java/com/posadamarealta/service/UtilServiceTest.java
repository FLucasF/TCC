package com.posadamarealta.service;

import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

public class UtilServiceTest {

    @Test
    public void testIsHighSeason() {
        assertTrue(UtilService.isHighSeason(LocalDate.of(2027, 7, 10)));
        assertTrue(UtilService.isHighSeason(LocalDate.of(2027, 12, 25)));
        assertTrue(UtilService.isHighSeason(LocalDate.of(2027, 1, 15)));
        assertTrue(UtilService.isHighSeason(LocalDate.of(2027, 2, 28)));

        assertFalse(UtilService.isHighSeason(LocalDate.of(2027, 3, 15)));
        assertFalse(UtilService.isHighSeason(LocalDate.of(2027, 6, 10)));
        assertFalse(UtilService.isHighSeason(LocalDate.of(2027, 11, 1)));
    }

    @Test
    public void testRoundHalfEven() {
        assertEquals(new BigDecimal("1.50"), UtilService.roundHalfEven(new BigDecimal("1.504")));
        assertEquals(new BigDecimal("1.50"), UtilService.roundHalfEven(new BigDecimal("1.505")));
        assertEquals(new BigDecimal("1.52"), UtilService.roundHalfEven(new BigDecimal("1.515")));
    }

    @Test
    public void testCalculateNightlyRate() {
        BigDecimal baseRate = new BigDecimal("100.00");

        BigDecimal highSeasonRate = UtilService.calculateNightlyRate(baseRate, LocalDate.of(2027, 7, 10));
        assertEquals(new BigDecimal("120.00"), highSeasonRate);

        BigDecimal lowSeasonRate = UtilService.calculateNightlyRate(baseRate, LocalDate.of(2027, 3, 15));
        assertEquals(new BigDecimal("100.00"), lowSeasonRate);
    }

    @Test
    public void testCalculateNights() {
        int nights = UtilService.calculateNights(LocalDate.of(2027, 7, 10), LocalDate.of(2027, 7, 12));
        assertEquals(2, nights);
    }

    @Test
    public void testFormatDate() {
        String formatted = UtilService.formatDate(LocalDate.of(2027, 3, 10));
        assertEquals("10/03/2027", formatted);
    }

    @Test
    public void testFormatMoney() {
        String formatted = UtilService.formatMoney(new BigDecimal("1234.56"));
        assertEquals("R$ 1.234,56", formatted);
    }

    @Test
    public void testParseDate() {
        LocalDate parsed = UtilService.parseDate("2027-07-10");
        assertEquals(LocalDate.of(2027, 7, 10), parsed);
    }
}
