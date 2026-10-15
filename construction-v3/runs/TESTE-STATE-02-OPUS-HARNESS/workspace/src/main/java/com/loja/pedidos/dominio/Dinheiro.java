package com.loja.pedidos.dominio;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Todo valor em dinheiro tem 2 casas decimais. */
public final class Dinheiro {

    private static final int CASAS_DECIMAIS = 2;

    public static final BigDecimal ZERO = comCasasDecimais(BigDecimal.ZERO);

    private Dinheiro() {
    }

    public static BigDecimal comCasasDecimais(BigDecimal valor) {
        return valor.setScale(CASAS_DECIMAIS, RoundingMode.HALF_UP);
    }
}
