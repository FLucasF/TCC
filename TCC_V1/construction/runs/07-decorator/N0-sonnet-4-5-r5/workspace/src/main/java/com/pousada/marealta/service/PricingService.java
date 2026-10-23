package com.pousada.marealta.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

/**
 * Serviço de cálculo de preços com suporte a alta temporada e arredondamento bancário.
 *
 * Alta temporada (20% adicional):
 * - 15 de dezembro até fim de fevereiro
 * - Mês de julho inteiro
 *
 * Arredondamento: "meio para o par" (banker's rounding / HALF_EVEN)
 */
public class PricingService {
    private static final BigDecimal HIGH_SEASON_MULTIPLIER = new BigDecimal("1.20");
    private static final int SCALE = 2;

    public boolean isHighSeason(LocalDate date) {
        int month = date.getMonthValue();
        int day = date.getDayOfMonth();

        // Julho inteiro
        if (month == 7) {
            return true;
        }

        // Dezembro (a partir do dia 15)
        if (month == 12 && day >= 15) {
            return true;
        }

        // Janeiro e fevereiro inteiros
        if (month == 1 || month == 2) {
            return true;
        }

        return false;
    }

    public BigDecimal calculateNightPrice(BigDecimal baseRate, LocalDate night) {
        BigDecimal price = baseRate;
        if (isHighSeason(night)) {
            price = price.multiply(HIGH_SEASON_MULTIPLIER);
        }
        return round(price);
    }

    public BigDecimal round(BigDecimal value) {
        return value.setScale(SCALE, RoundingMode.HALF_EVEN);
    }
}
