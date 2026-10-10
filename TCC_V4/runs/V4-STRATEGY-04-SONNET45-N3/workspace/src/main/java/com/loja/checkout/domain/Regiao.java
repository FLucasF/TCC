package com.loja.checkout.domain;

import com.loja.checkout.exception.CheckoutException;
import java.math.BigDecimal;

public enum Regiao {
    SUDESTE(new BigDecimal("0.01")),
    SUL(new BigDecimal("0.01")),
    CENTRO_OESTE(new BigDecimal("0.015")),
    NORTE(new BigDecimal("0.025")),
    NORDESTE(new BigDecimal("0.02"));

    private final BigDecimal percentualSeguro;

    Regiao(BigDecimal percentualSeguro) {
        this.percentualSeguro = percentualSeguro;
    }

    public BigDecimal calcularSeguro(BigDecimal subtotalProdutos) {
        return subtotalProdutos.multiply(percentualSeguro)
            .setScale(2, java.math.RoundingMode.HALF_EVEN);
    }

    public static Regiao fromString(String valor) {
        if (valor == null) {
            throw new CheckoutException("REGIAO_INVALIDA");
        }
        try {
            return Regiao.valueOf(valor);
        } catch (IllegalArgumentException e) {
            throw new CheckoutException("REGIAO_INVALIDA");
        }
    }
}
