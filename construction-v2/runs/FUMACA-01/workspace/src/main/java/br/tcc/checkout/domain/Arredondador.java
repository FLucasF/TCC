package br.tcc.checkout.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class Arredondador {
    public static double arredondar(double valor) {
        return new BigDecimal(valor)
            .setScale(2, RoundingMode.HALF_EVEN)
            .doubleValue();
    }
}
