package com.pousada.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

public class SeasonTest {
  @Test
  void testJulyIsHighSeason() {
    assertThat(Season.isHighSeason(LocalDate.of(2027, 7, 1))).isTrue();
    assertThat(Season.isHighSeason(LocalDate.of(2027, 7, 31))).isTrue();
  }

  @Test
  void testDecember15IsHighSeason() {
    assertThat(Season.isHighSeason(LocalDate.of(2027, 12, 14))).isFalse();
    assertThat(Season.isHighSeason(LocalDate.of(2027, 12, 15))).isTrue();
    assertThat(Season.isHighSeason(LocalDate.of(2027, 12, 31))).isTrue();
  }

  @Test
  void testJanuaryIsHighSeason() {
    assertThat(Season.isHighSeason(LocalDate.of(2027, 1, 1))).isTrue();
    assertThat(Season.isHighSeason(LocalDate.of(2027, 1, 31))).isTrue();
  }

  @Test
  void testFebruaryIsHighSeason() {
    assertThat(Season.isHighSeason(LocalDate.of(2027, 2, 1))).isTrue();
    assertThat(Season.isHighSeason(LocalDate.of(2027, 2, 28))).isTrue();
  }

  @Test
  void testMarchIsNotHighSeason() {
    assertThat(Season.isHighSeason(LocalDate.of(2027, 3, 1))).isFalse();
    assertThat(Season.isHighSeason(LocalDate.of(2027, 3, 31))).isFalse();
  }
}
