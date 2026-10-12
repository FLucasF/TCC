package com.loja.checkout.regiao;

import com.loja.checkout.exception.CheckoutException;

import java.math.BigDecimal;

public enum RegiaoSeguro {
    SUDESTE(new BigDecimal("0.01")),
    SUL(new BigDecimal("0.01")),
    CENTRO_OESTE(new BigDecimal("0.015")),
    NORTE(new BigDecimal("0.025")),
    NORDESTE(new BigDecimal("0.02"));

    private final BigDecimal percentual;

    RegiaoSeguro(BigDecimal percentual) {
        this.percentual = percentual;
    }

    public BigDecimal getPercentual() {
        return percentual;
    }

    public static RegiaoSeguro parse(String codigo) {
        if (codigo == null) throw new CheckoutException("REGIAO_INVALIDA");
        try {
            return valueOf(codigo);
        } catch (IllegalArgumentException e) {
            throw new CheckoutException("REGIAO_INVALIDA");
        }
    }
}
