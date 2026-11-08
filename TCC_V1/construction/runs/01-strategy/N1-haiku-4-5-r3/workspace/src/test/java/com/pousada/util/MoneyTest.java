package com.pousada.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

public class MoneyTest {
  @Test
  void testRoundingHalfEven() {
    assertThat(Money.round(new BigDecimal("1.125"))).isEqualByComparingTo(new BigDecimal("1.12"));
    assertThat(Money.round(new BigDecimal("1.135"))).isEqualByComparingTo(new BigDecimal("1.14"));
    assertThat(Money.round(new BigDecimal("1.145"))).isEqualByComparingTo(new BigDecimal("1.14"));
    assertThat(Money.round(new BigDecimal("1.155"))).isEqualByComparingTo(new BigDecimal("1.16"));
  }

  @Test
  void testPercentage() {
    BigDecimal value = new BigDecimal("779.80");
    BigDecimal discount = Money.percentage(value, 15);
    assertThat(discount).isEqualByComparingTo(new BigDecimal("116.97"));
  }

  @Test
  void testMultiply() {
    BigDecimal rate = new BigDecimal("249.90");
    BigDecimal highSeasonRate = Money.multiply(rate, BigDecimal.valueOf(1.2));
    assertThat(highSeasonRate).isEqualByComparingTo(new BigDecimal("299.88"));
  }
}
