package com.pousadamarealta;

import org.junit.jupiter.api.Test;
import com.pousadamarealta.util.PriceCalculator;
import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

public class RoundingTest {

    @Test
    public void shouldRoundHalfToEven() {
        BigDecimal value1 = new BigDecimal("10.015");
        BigDecimal rounded1 = PriceCalculator.roundHalfToEven(value1, 2);
        assertThat(rounded1).isEqualTo(new BigDecimal("10.02"));

        BigDecimal value2 = new BigDecimal("10.025");
        BigDecimal rounded2 = PriceCalculator.roundHalfToEven(value2, 2);
        assertThat(rounded2).isEqualTo(new BigDecimal("10.02"));

        BigDecimal value3 = new BigDecimal("10.035");
        BigDecimal rounded3 = PriceCalculator.roundHalfToEven(value3, 2);
        assertThat(rounded3).isEqualTo(new BigDecimal("10.04"));

        BigDecimal value4 = new BigDecimal("10.045");
        BigDecimal rounded4 = PriceCalculator.roundHalfToEven(value4, 2);
        assertThat(rounded4).isEqualTo(new BigDecimal("10.04"));
    }

    @Test
    public void shouldCalculateCorrectPriceForExampleInSpecification() {
        BigDecimal baseRate = new BigDecimal("249.90");
        java.time.LocalDate date1 = java.time.LocalDate.of(2027, 7, 10);
        java.time.LocalDate date2 = java.time.LocalDate.of(2027, 7, 11);

        BigDecimal price1 = PriceCalculator.calculateNightlyPrice(baseRate, date1);
        BigDecimal price2 = PriceCalculator.calculateNightlyPrice(baseRate, date2);

        assertThat(price1).isEqualTo(new BigDecimal("299.88"));
        assertThat(price2).isEqualTo(new BigDecimal("299.88"));

        BigDecimal subtotal = price1.add(price2);
        assertThat(subtotal).isEqualTo(new BigDecimal("599.76"));
    }
}
