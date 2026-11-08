package pousada.domain;

import java.math.BigDecimal;
import java.time.LocalDate;

public enum Season {
    HIGH("1.20"),
    LOW("1.00");

    private final BigDecimal multiplier;

    Season(String multiplier) {
        this.multiplier = new BigDecimal(multiplier);
    }

    /** Alta temporada: de 15 de dezembro ao fim de fevereiro, e todo o mês de julho. */
    public static Season of(LocalDate night) {
        int month = night.getMonthValue();
        boolean high = month == 1 || month == 2 || month == 7
                || (month == 12 && night.getDayOfMonth() >= 15);
        return high ? HIGH : LOW;
    }

    public BigDecimal apply(BigDecimal rate) {
        return Money.round(rate.multiply(multiplier));
    }
}
