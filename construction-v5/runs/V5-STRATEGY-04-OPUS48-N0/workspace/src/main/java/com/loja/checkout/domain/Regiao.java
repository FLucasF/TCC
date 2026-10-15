package com.loja.checkout.domain;

import java.math.BigDecimal;

/**
 * Região do cliente. Todo pedido leva seguro contra extravio e roubo, e a
 * seguradora cobra um percentual por região. A conta é sempre a mesma — o
 * percentual sobre o valor dos produtos (sem desconto e sem frete) —, só o
 * percentual muda.
 */
public enum Regiao {

    SUDESTE(new BigDecimal("1")),
    SUL(new BigDecimal("1")),
    CENTRO_OESTE(new BigDecimal("1.5")),
    NORTE(new BigDecimal("2.5")),
    NORDESTE(new BigDecimal("2"));

    private final BigDecimal seguroPercent;

    Regiao(BigDecimal seguroPercent) {
        this.seguroPercent = seguroPercent;
    }

    /** Seguro do envio: percentual da região sobre o valor dos produtos, em centavos. */
    public BigDecimal seguro(BigDecimal subtotalProdutos) {
        return Money.percent(subtotalProdutos, seguroPercent);
    }
}
