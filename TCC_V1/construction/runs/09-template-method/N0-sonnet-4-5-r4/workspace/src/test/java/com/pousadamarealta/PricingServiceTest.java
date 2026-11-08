package com.pousadamarealta;

import com.pousadamarealta.service.PricingService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class PricingServiceTest {

    private final PricingService pricingService = new PricingService();

    @Test
    void testHighSeasonDetection() {
        assertThat(pricingService.isHighSeason(LocalDate.of(2027, 12, 15))).isTrue();
        assertThat(pricingService.isHighSeason(LocalDate.of(2027, 12, 31))).isTrue();
        assertThat(pricingService.isHighSeason(LocalDate.of(2027, 1, 1))).isTrue();
        assertThat(pricingService.isHighSeason(LocalDate.of(2027, 2, 28))).isTrue();
        assertThat(pricingService.isHighSeason(LocalDate.of(2027, 7, 1))).isTrue();
        assertThat(pricingService.isHighSeason(LocalDate.of(2027, 7, 31))).isTrue();

        assertThat(pricingService.isHighSeason(LocalDate.of(2027, 12, 14))).isFalse();
        assertThat(pricingService.isHighSeason(LocalDate.of(2027, 3, 1))).isFalse();
        assertThat(pricingService.isHighSeason(LocalDate.of(2027, 6, 30))).isFalse();
        assertThat(pricingService.isHighSeason(LocalDate.of(2027, 8, 1))).isFalse();
    }

    @Test
    void testHighSeasonPricing() {
        BigDecimal baseRate = new BigDecimal("249.90");
        LocalDate highSeasonDate = LocalDate.of(2027, 7, 10);

        BigDecimal price = pricingService.calculateNightPrice(baseRate, highSeasonDate);

        assertThat(price).isEqualByComparingTo(new BigDecimal("299.88"));
    }

    @Test
    void testBankersRounding() {
        assertThat(pricingService.roundBankers(new BigDecimal("1.125"))).isEqualByComparingTo(new BigDecimal("1.12"));
        assertThat(pricingService.roundBankers(new BigDecimal("1.135"))).isEqualByComparingTo(new BigDecimal("1.14"));
        assertThat(pricingService.roundBankers(new BigDecimal("1.145"))).isEqualByComparingTo(new BigDecimal("1.14"));
        assertThat(pricingService.roundBankers(new BigDecimal("1.155"))).isEqualByComparingTo(new BigDecimal("1.16"));
    }
}
