package br.com.marealta;

import java.math.BigDecimal;

public record Room(int number, String type, int capacity, BigDecimal nightlyRate) {}
