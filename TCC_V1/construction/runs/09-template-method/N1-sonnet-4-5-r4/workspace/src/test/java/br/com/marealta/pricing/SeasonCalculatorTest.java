package br.com.marealta.pricing;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class SeasonCalculatorTest {
    @Test
    void shouldBeHighSeasonInJuly() {
        LocalDate date = LocalDate.of(2027, 7, 1);
        assertThat(SeasonCalculator.isHighSeason(date)).isTrue();

        date = LocalDate.of(2027, 7, 15);
        assertThat(SeasonCalculator.isHighSeason(date)).isTrue();

        date = LocalDate.of(2027, 7, 31);
        assertThat(SeasonCalculator.isHighSeason(date)).isTrue();
    }

    @Test
    void shouldBeHighSeasonInJanuary() {
        LocalDate date = LocalDate.of(2027, 1, 1);
        assertThat(SeasonCalculator.isHighSeason(date)).isTrue();

        date = LocalDate.of(2027, 1, 31);
        assertThat(SeasonCalculator.isHighSeason(date)).isTrue();
    }

    @Test
    void shouldBeHighSeasonInFebruary() {
        LocalDate date = LocalDate.of(2027, 2, 1);
        assertThat(SeasonCalculator.isHighSeason(date)).isTrue();

        date = LocalDate.of(2027, 2, 28);
        assertThat(SeasonCalculator.isHighSeason(date)).isTrue();
    }

    @Test
    void shouldBeHighSeasonFromDecember15() {
        LocalDate date = LocalDate.of(2027, 12, 15);
        assertThat(SeasonCalculator.isHighSeason(date)).isTrue();

        date = LocalDate.of(2027, 12, 20);
        assertThat(SeasonCalculator.isHighSeason(date)).isTrue();

        date = LocalDate.of(2027, 12, 31);
        assertThat(SeasonCalculator.isHighSeason(date)).isTrue();
    }

    @Test
    void shouldNotBeHighSeasonBeforeDecember15() {
        LocalDate date = LocalDate.of(2027, 12, 14);
        assertThat(SeasonCalculator.isHighSeason(date)).isFalse();
    }

    @Test
    void shouldNotBeHighSeasonInMarch() {
        LocalDate date = LocalDate.of(2027, 3, 1);
        assertThat(SeasonCalculator.isHighSeason(date)).isFalse();
    }

    @Test
    void shouldNotBeHighSeasonInApril() {
        LocalDate date = LocalDate.of(2027, 4, 15);
        assertThat(SeasonCalculator.isHighSeason(date)).isFalse();
    }
}
