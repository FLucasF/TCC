package com.loja.checkout.dominio;

import java.math.BigDecimal;

/** A conta do imposto e a mesma em todas as regioes: so muda a aliquota. */
public enum Regiao {

    SUDESTE("0.12"),
    SUL("0.11"),
    CENTRO_OESTE("0.09"),
    NORTE("0.07"),
    NORDESTE("0.07");

    private final BigDecimal aliquota;

    Regiao(String aliquota) {
        this.aliquota = new BigDecimal(aliquota);
    }

    public BigDecimal imposto(BigDecimal produtosComDesconto) {
        return Dinheiro.centavos(produtosComDesconto.multiply(aliquota));
    }
}
