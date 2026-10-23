package com.pousada.marealta;

import com.pousada.marealta.service.PricingService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class PricingServiceTest {
    private final PricingService pricingService = new PricingService();

    @Test
    void testHighSeasonJuly() {
        assertTrue(pricingService.isHighSeason(LocalDate.of(2027, 7, 1)));
        assertTrue(pricingService.isHighSeason(LocalDate.of(2027, 7, 15)));
        assertTrue(pricingService.isHighSeason(LocalDate.of(2027, 7, 31)));
    }

    @Test
    void testHighSeasonDecemberToFebruary() {
        assertFalse(pricingService.isHighSeason(LocalDate.of(2027, 12, 14)));
        assertTrue(pricingService.isHighSeason(LocalDate.of(2027, 12, 15)));
        assertTrue(pricingService.isHighSeason(LocalDate.of(2027, 12, 31)));
        assertTrue(pricingService.isHighSeason(LocalDate.of(2028, 1, 1)));
        assertTrue(pricingService.isHighSeason(LocalDate.of(2028, 1, 15)));
        assertTrue(pricingService.isHighSeason(LocalDate.of(2028, 2, 1)));
        assertTrue(pricingService.isHighSeason(LocalDate.of(2028, 2, 28)));
    }

    @Test
    void testLowSeason() {
        assertFalse(pricingService.isHighSeason(LocalDate.of(2027, 3, 10)));
        assertFalse(pricingService.isHighSeason(LocalDate.of(2027, 6, 15)));
        assertFalse(pricingService.isHighSeason(LocalDate.of(2027, 9, 1)));
    }

    @Test
    void testCalculateNightPriceHighSeason() {
        BigDecimal baseRate = new BigDecimal("249.90");
        LocalDate highSeasonNight = LocalDate.of(2027, 7, 10);

        // 249.90 * 1.20 = 299.88
        BigDecimal expected = new BigDecimal("299.88");
        BigDecimal actual = pricingService.calculateNightPrice(baseRate, highSeasonNight);

        assertEquals(expected, actual);
    }

    @Test
    void testCalculateNightPriceLowSeason() {
        BigDecimal baseRate = new BigDecimal("249.90");
        LocalDate lowSeasonNight = LocalDate.of(2027, 3, 10);

        BigDecimal expected = new BigDecimal("249.90");
        BigDecimal actual = pricingService.calculateNightPrice(baseRate, lowSeasonNight);

        assertEquals(expected, actual);
    }

    @Test
    void testBankersRounding() {
        // Testa arredondamento "meio para o par" (HALF_EVEN)
        // 0.125 arredonda para 0.12 (par)
        // 0.135 arredonda para 0.14 (par)

        assertEquals(new BigDecimal("0.12"), pricingService.round(new BigDecimal("0.125")));
        assertEquals(new BigDecimal("0.14"), pricingService.round(new BigDecimal("0.135")));
    }
}
