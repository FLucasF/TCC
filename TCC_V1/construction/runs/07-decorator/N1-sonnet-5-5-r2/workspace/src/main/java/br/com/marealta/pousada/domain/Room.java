package br.com.marealta.pousada.domain;

import java.math.BigDecimal;

public record Room(int number, String type, int capacity, BigDecimal nightlyRate) {
}
