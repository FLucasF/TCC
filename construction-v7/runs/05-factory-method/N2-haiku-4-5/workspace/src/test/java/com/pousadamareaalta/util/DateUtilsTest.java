package com.pousadamareaalta.util;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.*;

class DateUtilsTest {
    @Test
    void shouldCalculateNights() {
        var checkIn = LocalDate.of(2027, 3, 10);
        var checkOut = LocalDate.of(2027, 3, 12);

        int nights = DateUtils.calculateNights(checkIn, checkOut);

        assertThat(nights).isEqualTo(2);
    }

    @Test
    void shouldHandleSingleNight() {
        var checkIn = LocalDate.of(2027, 3, 10);
        var checkOut = LocalDate.of(2027, 3, 11);

        int nights = DateUtils.calculateNights(checkIn, checkOut);

        assertThat(nights).isEqualTo(1);
    }

    @Test
    void shouldIdentifyHighSeasonJuly() {
        assertThat(DateUtils.isHighSeason(LocalDate.of(2027, 7, 15))).isTrue();
    }

    @Test
    void shouldIdentifyHighSeasonDecember() {
        assertThat(DateUtils.isHighSeason(LocalDate.of(2027, 12, 25))).isTrue();
    }

    @Test
    void shouldIdentifyHighSeasonJanuary() {
        assertThat(DateUtils.isHighSeason(LocalDate.of(2027, 1, 15))).isTrue();
    }

    @Test
    void shouldIdentifyHighSeasonFebruary() {
        assertThat(DateUtils.isHighSeason(LocalDate.of(2027, 2, 15))).isTrue();
    }

    @Test
    void shouldNotIdentifyLowSeasonAsHighSeason() {
        assertThat(DateUtils.isHighSeason(LocalDate.of(2027, 5, 15))).isFalse();
        assertThat(DateUtils.isHighSeason(LocalDate.of(2027, 11, 10))).isFalse();
    }

    @Test
    void shouldFormatDate() {
        var date = LocalDate.of(2027, 3, 10);
        String formatted = DateUtils.formatDate(date);
        assertThat(formatted).isEqualTo("10/03/2027");
    }

    @Test
    void shouldFormatDateIso() {
        var date = LocalDate.of(2027, 3, 10);
        String formatted = DateUtils.formatDateIso(date);
        assertThat(formatted).isEqualTo("2027-03-10");
    }

    @Test
    void shouldParseDate() {
        LocalDate date = DateUtils.parseDate("2027-03-10");
        assertThat(date).isEqualTo(LocalDate.of(2027, 3, 10));
    }
}
