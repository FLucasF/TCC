package com.pousada.marealta.service;

import com.pousada.marealta.domain.RatePlan;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class PricingCalculatorTest {

  @Test
  void testPeakSeasonDates() {
    assertTrue(PricingCalculator.isPeakSeason(LocalDate.of(2027, 7, 10)));
    assertTrue(PricingCalculator.isPeakSeason(LocalDate.of(2027, 12, 20)));
    assertTrue(PricingCalculator.isPeakSeason(LocalDate.of(2027, 1, 15)));
    assertTrue(PricingCalculator.isPeakSeason(LocalDate.of(2027, 2, 28)));

    assertFalse(PricingCalculator.isPeakSeason(LocalDate.of(2027, 3, 15)));
    assertFalse(PricingCalculator.isPeakSeason(LocalDate.of(2027, 6, 15)));
  }

  @Test
  void testNightlyRateCalculation() {
    BigDecimal baseRate = new BigDecimal("249.90");

    BigDecimal peakRate = PricingCalculator.calculateNightlyRate(baseRate, LocalDate.of(2027, 7, 10));
    assertEquals(0, peakRate.compareTo(new BigDecimal("299.88")));

    BigDecimal normalRate = PricingCalculator.calculateNightlyRate(baseRate, LocalDate.of(2027, 3, 10));
    assertEquals(0, normalRate.compareTo(new BigDecimal("249.90")));
  }

  @Test
  void testSubtotalCalculation() {
    BigDecimal baseRate = new BigDecimal("249.90");

    BigDecimal subtotal = PricingCalculator.calculateSubtotal(baseRate, LocalDate.of(2027, 7, 10), LocalDate.of(2027, 7, 12));
    assertEquals(0, subtotal.compareTo(new BigDecimal("599.76")));
  }

  @Test
  void testDiscountCalculation() {
    BigDecimal subtotal = new BigDecimal("599.76");

    BigDecimal noDiscount = PricingCalculator.calculateDiscount(subtotal, 0.0);
    assertEquals(0, noDiscount.compareTo(BigDecimal.ZERO));

    BigDecimal withDiscount = PricingCalculator.calculateDiscount(subtotal, 0.15);
    assertEquals(0, withDiscount.compareTo(new BigDecimal("89.96")));
  }

  @Test
  void testTotalCalculation() {
    BigDecimal subtotal = new BigDecimal("599.76");
    BigDecimal discount = new BigDecimal("89.96");

    BigDecimal total = PricingCalculator.calculateTotal(subtotal, discount);
    assertEquals(0, total.compareTo(new BigDecimal("509.80")));
  }

  @Test
  void testNightsCalculation() {
    int nights = PricingCalculator.calculateNights(LocalDate.of(2027, 7, 10), LocalDate.of(2027, 7, 12));
    assertEquals(2, nights);
  }
}
