package com.loja.pedidos.dominio;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Valores em dinheiro sempre com 2 casas decimais. */
public final class Dinheiro {

    public static final BigDecimal ZERO = normalizar(BigDecimal.ZERO);

    private Dinheiro() {
    }

    public static BigDecimal normalizar(BigDecimal valor) {
        return valor.setScale(2, RoundingMode.HALF_UP);
    }

    /** Subtracao que nunca fica negativa: usada para descontar taxas do reembolso. */
    public static BigDecimal subtrairSemNegativo(BigDecimal valor, BigDecimal aDescontar) {
        BigDecimal resultado = valor.subtract(aDescontar);
        return normalizar(resultado.signum() < 0 ? BigDecimal.ZERO : resultado);
    }
}
