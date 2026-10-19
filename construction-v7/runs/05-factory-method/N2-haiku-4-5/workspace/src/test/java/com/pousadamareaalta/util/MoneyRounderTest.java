package com.pousadamareaalta.util;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.*;

class MoneyRounderTest {
    @Test
    void shouldRoundHalfEven() {
        BigDecimal value = new BigDecimal("1.235");
        BigDecimal rounded = MoneyRounder.round(value);
        assertThat(rounded).isEqualByComparingTo(new BigDecimal("1.24"));
    }

    @Test
    void shouldRoundDownWhenBelowHalf() {
        BigDecimal value = new BigDecimal("1.234");
        BigDecimal rounded = MoneyRounder.round(value);
        assertThat(rounded).isEqualByComparingTo(new BigDecimal("1.23"));
    }

    @Test
    void shouldRoundToEvenWhenExactlyHalf() {
        BigDecimal value1 = new BigDecimal("1.225");
        BigDecimal rounded1 = MoneyRounder.round(value1);
        assertThat(rounded1).isEqualByComparingTo(new BigDecimal("1.22"));

        BigDecimal value2 = new BigDecimal("1.235");
        BigDecimal rounded2 = MoneyRounder.round(value2);
        assertThat(rounded2).isEqualByComparingTo(new BigDecimal("1.24"));
    }

    @Test
    void shouldMaintainTwoDecimalPlaces() {
        BigDecimal value = new BigDecimal("1.2");
        BigDecimal rounded = MoneyRounder.round(value);
        assertThat(rounded.scale()).isEqualTo(2);
        assertThat(rounded).isEqualByComparingTo(new BigDecimal("1.20"));
    }
}
