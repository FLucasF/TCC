package com.loja.checkout.dominio;

import java.math.BigDecimal;

/**
 * O seguro é a mesma conta em toda região: a alíquota sobre o valor dos produtos.
 * Só a alíquota muda de região para região.
 */
public enum Regiao {

    SUDESTE("0.01"),
    SUL("0.01"),
    CENTRO_OESTE("0.015"),
    NORTE("0.025"),
    NORDESTE("0.02");

    private final BigDecimal aliquota;

    Regiao(String aliquota) {
        this.aliquota = Dinheiro.taxa(aliquota);
    }

    public BigDecimal seguro(BigDecimal subtotalProdutos) {
        return Dinheiro.percentual(subtotalProdutos, aliquota);
    }
}
