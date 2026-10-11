package com.loja.checkout.regiao;

import com.loja.checkout.calculo.Dinheiro;

import java.math.BigDecimal;
import java.util.Optional;

public enum Regiao {
    SUDESTE(new BigDecimal("0.010")),
    SUL(new BigDecimal("0.010")),
    CENTRO_OESTE(new BigDecimal("0.015")),
    NORTE(new BigDecimal("0.025")),
    NORDESTE(new BigDecimal("0.020"));

    private final BigDecimal percentualSeguro;

    Regiao(BigDecimal percentualSeguro) {
        this.percentualSeguro = percentualSeguro;
    }

    public BigDecimal seguro(BigDecimal subtotal) {
        return Dinheiro.cents(subtotal.multiply(percentualSeguro));
    }

    public static Optional<Regiao> por(String codigo) {
        if (codigo == null) return Optional.empty();
        try {
            return Optional.of(Regiao.valueOf(codigo));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
