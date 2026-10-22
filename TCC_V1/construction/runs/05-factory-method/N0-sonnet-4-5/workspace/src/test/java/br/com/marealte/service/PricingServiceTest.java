package br.com.marealte.service;

import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.LocalDate;
import static org.junit.jupiter.api.Assertions.*;

class PricingServiceTest {

    @Test
    void shouldIdentifyHighSeasonInJuly() {
        assertTrue(PricingService.isHighSeason(LocalDate.of(2027, 7, 1)));
        assertTrue(PricingService.isHighSeason(LocalDate.of(2027, 7, 15)));
        assertTrue(PricingService.isHighSeason(LocalDate.of(2027, 7, 31)));
    }

    @Test
    void shouldIdentifyHighSeasonInDecemberFrom15th() {
        assertFalse(PricingService.isHighSeason(LocalDate.of(2026, 12, 14)));
        assertTrue(PricingService.isHighSeason(LocalDate.of(2026, 12, 15)));
        assertTrue(PricingService.isHighSeason(LocalDate.of(2026, 12, 31)));
    }

    @Test
    void shouldIdentifyHighSeasonInJanuaryAndFebruary() {
        assertTrue(PricingService.isHighSeason(LocalDate.of(2027, 1, 1)));
        assertTrue(PricingService.isHighSeason(LocalDate.of(2027, 1, 15)));
        assertTrue(PricingService.isHighSeason(LocalDate.of(2027, 2, 1)));
        assertTrue(PricingService.isHighSeason(LocalDate.of(2027, 2, 28)));
    }

    @Test
    void shouldNotBeHighSeasonInMarch() {
        assertFalse(PricingService.isHighSeason(LocalDate.of(2027, 3, 1)));
        assertFalse(PricingService.isHighSeason(LocalDate.of(2027, 3, 15)));
    }

    @Test
    void shouldCalculateRegularSeasonPrice() {
        BigDecimal baseRate = new BigDecimal("249.90");
        LocalDate regularDate = LocalDate.of(2027, 3, 10);

        BigDecimal price = PricingService.calculateNightPrice(baseRate, regularDate);

        assertEquals(new BigDecimal("249.90"), price);
    }

    @Test
    void shouldCalculateHighSeasonPriceWithRounding() {
        BigDecimal baseRate = new BigDecimal("249.90");
        LocalDate highSeasonDate = LocalDate.of(2027, 7, 10);

        BigDecimal price = PricingService.calculateNightPrice(baseRate, highSeasonDate);

        assertEquals(new BigDecimal("299.88"), price);
    }

    @Test
    void shouldApplyHalfEvenRounding() {
        BigDecimal value1 = new BigDecimal("10.125");
        BigDecimal value2 = new BigDecimal("10.135");

        BigDecimal rounded1 = PricingService.round(value1);
        BigDecimal rounded2 = PricingService.round(value2);

        assertEquals(new BigDecimal("10.12"), rounded1);
        assertEquals(new BigDecimal("10.14"), rounded2);
    }

    @Test
    void shouldCalculateDeluxeHighSeasonPrice() {
        BigDecimal baseRate = new BigDecimal("389.90");
        LocalDate highSeasonDate = LocalDate.of(2027, 7, 10);

        BigDecimal price = PricingService.calculateNightPrice(baseRate, highSeasonDate);

        assertEquals(new BigDecimal("467.88"), price);
    }

    @Test
    void shouldCalculateSuiteHighSeasonPrice() {
        BigDecimal baseRate = new BigDecimal("579.90");
        LocalDate highSeasonDate = LocalDate.of(2027, 7, 10);

        BigDecimal price = PricingService.calculateNightPrice(baseRate, highSeasonDate);

        assertEquals(new BigDecimal("695.88"), price);
    }
}
