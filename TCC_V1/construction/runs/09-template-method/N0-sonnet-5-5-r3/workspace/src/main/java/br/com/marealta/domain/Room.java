package br.com.marealta.domain;

import java.math.BigDecimal;

public record Room(int number, RoomType type, int capacity, BigDecimal nightlyRate) {}
