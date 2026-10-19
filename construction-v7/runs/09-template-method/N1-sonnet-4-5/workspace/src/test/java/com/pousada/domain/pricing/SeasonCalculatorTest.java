package com.pousada.domain.pricing;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class SeasonCalculatorTest {

    @Test
    void shouldIdentifyJulyAsHighSeason() {
        assertThat(SeasonCalculator.isHighSeason(LocalDate.of(2027, 7, 1))).isTrue();
        assertThat(SeasonCalculator.isHighSeason(LocalDate.of(2027, 7, 15))).isTrue();
        assertThat(SeasonCalculator.isHighSeason(LocalDate.of(2027, 7, 31))).isTrue();
    }

    @Test
    void shouldIdentifyDecember15OnwardsAsHighSeason() {
        assertThat(SeasonCalculator.isHighSeason(LocalDate.of(2026, 12, 14))).isFalse();
        assertThat(SeasonCalculator.isHighSeason(LocalDate.of(2026, 12, 15))).isTrue();
        assertThat(SeasonCalculator.isHighSeason(LocalDate.of(2026, 12, 31))).isTrue();
    }

    @Test
    void shouldIdentifyJanuaryAndFebruaryAsHighSeason() {
        assertThat(SeasonCalculator.isHighSeason(LocalDate.of(2027, 1, 1))).isTrue();
        assertThat(SeasonCalculator.isHighSeason(LocalDate.of(2027, 1, 31))).isTrue();
        assertThat(SeasonCalculator.isHighSeason(LocalDate.of(2027, 2, 1))).isTrue();
        assertThat(SeasonCalculator.isHighSeason(LocalDate.of(2027, 2, 28))).isTrue();
    }

    @Test
    void shouldIdentifyMarchAsRegularSeason() {
        assertThat(SeasonCalculator.isHighSeason(LocalDate.of(2027, 3, 1))).isFalse();
        assertThat(SeasonCalculator.isHighSeason(LocalDate.of(2027, 3, 15))).isFalse();
    }
}
