package com.loja.checkout.service;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class ArredondamentoUtil {

    public static BigDecimal arredondar(BigDecimal valor) {
        return valor.setScale(2, RoundingMode.HALF_EVEN);
    }
}
