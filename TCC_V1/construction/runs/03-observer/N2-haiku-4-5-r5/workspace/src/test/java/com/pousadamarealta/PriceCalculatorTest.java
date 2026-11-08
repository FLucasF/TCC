package com.pousadamarealta;

import com.pousadamarealta.infrastructure.PriceCalculator;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class PriceCalculatorTest {
  @Test
  public void testHighSeasonInJuly() {
    assertTrue(PriceCalculator.isHighSeason(LocalDate.of(2027, 7, 10)));
  }

  @Test
  public void testHighSeasonInDecember() {
    assertTrue(PriceCalculator.isHighSeason(LocalDate.of(2027, 12, 15)));
    assertFalse(PriceCalculator.isHighSeason(LocalDate.of(2027, 12, 14)));
  }

  @Test
  public void testHighSeasonInJanuary() {
    assertTrue(PriceCalculator.isHighSeason(LocalDate.of(2027, 1, 15)));
  }

  @Test
  public void testLowSeason() {
    assertFalse(PriceCalculator.isHighSeason(LocalDate.of(2027, 5, 15)));
  }

  @Test
  public void testNightlyPriceWithHighSeasonMultiplier() {
    BigDecimal baseRate = BigDecimal.valueOf(249.90);
    BigDecimal price = PriceCalculator.calculateNightlyPrice(baseRate, LocalDate.of(2027, 7, 10));
    assertEquals(new BigDecimal("299.88"), price);
  }

  @Test
  public void testExample() {
    BigDecimal baseRate = BigDecimal.valueOf(249.90);
    BigDecimal nightlyPrice = PriceCalculator.calculateNightlyPrice(baseRate, LocalDate.of(2027, 7, 10));
    assertEquals(new BigDecimal("299.88"), nightlyPrice);

    BigDecimal subtotal = nightlyPrice.multiply(BigDecimal.valueOf(2));
    subtotal = PriceCalculator.round(subtotal);
    assertEquals(new BigDecimal("599.76"), subtotal);
  }
}
