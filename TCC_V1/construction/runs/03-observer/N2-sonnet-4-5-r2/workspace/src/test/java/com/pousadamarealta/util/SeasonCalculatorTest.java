package com.pousadamarealta.util;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class SeasonCalculatorTest {

    @Test
    void julyIsHighSeason() {
        assertThat(SeasonCalculator.isHighSeason(LocalDate.of(2027, 7, 1))).isTrue();
        assertThat(SeasonCalculator.isHighSeason(LocalDate.of(2027, 7, 15))).isTrue();
        assertThat(SeasonCalculator.isHighSeason(LocalDate.of(2027, 7, 31))).isTrue();
    }

    @Test
    void decemberFrom15IsHighSeason() {
        assertThat(SeasonCalculator.isHighSeason(LocalDate.of(2027, 12, 14))).isFalse();
        assertThat(SeasonCalculator.isHighSeason(LocalDate.of(2027, 12, 15))).isTrue();
        assertThat(SeasonCalculator.isHighSeason(LocalDate.of(2027, 12, 25))).isTrue();
        assertThat(SeasonCalculator.isHighSeason(LocalDate.of(2027, 12, 31))).isTrue();
    }

    @Test
    void januaryAndFebruaryAreHighSeason() {
        assertThat(SeasonCalculator.isHighSeason(LocalDate.of(2027, 1, 1))).isTrue();
        assertThat(SeasonCalculator.isHighSeason(LocalDate.of(2027, 1, 15))).isTrue();
        assertThat(SeasonCalculator.isHighSeason(LocalDate.of(2027, 1, 31))).isTrue();

        assertThat(SeasonCalculator.isHighSeason(LocalDate.of(2027, 2, 1))).isTrue();
        assertThat(SeasonCalculator.isHighSeason(LocalDate.of(2027, 2, 15))).isTrue();
        assertThat(SeasonCalculator.isHighSeason(LocalDate.of(2027, 2, 28))).isTrue();
    }

    @Test
    void otherMonthsAreLowSeason() {
        assertThat(SeasonCalculator.isHighSeason(LocalDate.of(2027, 3, 1))).isFalse();
        assertThat(SeasonCalculator.isHighSeason(LocalDate.of(2027, 4, 15))).isFalse();
        assertThat(SeasonCalculator.isHighSeason(LocalDate.of(2027, 5, 20))).isFalse();
        assertThat(SeasonCalculator.isHighSeason(LocalDate.of(2027, 6, 10))).isFalse();
        assertThat(SeasonCalculator.isHighSeason(LocalDate.of(2027, 8, 5))).isFalse();
        assertThat(SeasonCalculator.isHighSeason(LocalDate.of(2027, 9, 12))).isFalse();
        assertThat(SeasonCalculator.isHighSeason(LocalDate.of(2027, 10, 30))).isFalse();
        assertThat(SeasonCalculator.isHighSeason(LocalDate.of(2027, 11, 11))).isFalse();
    }
}
