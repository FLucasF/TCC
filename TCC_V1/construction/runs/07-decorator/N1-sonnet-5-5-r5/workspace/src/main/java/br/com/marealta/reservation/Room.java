package br.com.marealta.reservation;

import java.math.BigDecimal;

public record Room(int number, String type, int capacity, BigDecimal nightlyRate) {
}
