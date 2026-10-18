package com.loja.order.util;

import org.junit.jupiter.api.Test;
import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class RoundingUtilTest {
  @Test
  void shouldRoundHalfEvenCorrectly() {
    assertEquals(new BigDecimal("3.00"), RoundingUtil.roundHalfEven(new BigDecimal("2.995")));
    assertEquals(new BigDecimal("2.98"), RoundingUtil.roundHalfEven(new BigDecimal("2.985")));
    assertEquals(new BigDecimal("2.50"), RoundingUtil.roundHalfEven(new BigDecimal("2.505")));
    assertEquals(new BigDecimal("2.50"), RoundingUtil.roundHalfEven(new BigDecimal("2.495")));
  }

  @Test
  void shouldCeilDaysCorrectly() {
    assertEquals(2, RoundingUtil.ceilDays(41));
    assertEquals(3, RoundingUtil.ceilDays(49));
    assertEquals(3, RoundingUtil.ceilDays(50));
    assertEquals(1, RoundingUtil.ceilDays(24));
    assertEquals(1, RoundingUtil.ceilDays(0.5));
  }
}
