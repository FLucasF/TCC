package br.tcc.checkout.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class MoedaUtil {
    public static BigDecimal arredondar(BigDecimal valor) {
        if (valor == null) {
            return BigDecimal.ZERO;
        }
        return valor.setScale(2, RoundingMode.HALF_EVEN);
    }
}
