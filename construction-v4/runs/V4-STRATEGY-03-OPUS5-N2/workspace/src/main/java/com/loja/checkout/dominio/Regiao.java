package com.loja.checkout.dominio;

import java.math.BigDecimal;

/**
 * Regiao do cliente. A conta do seguro e a mesma em todas as regioes (um
 * percentual sobre o valor dos produtos), so o percentual muda.
 */
public enum Regiao {

    SUDESTE("0.01"),
    SUL("0.01"),
    CENTRO_OESTE("0.015"),
    NORTE("0.025"),
    NORDESTE("0.02");

    private final BigDecimal percentualSeguro;

    Regiao(String percentualSeguro) {
        this.percentualSeguro = new BigDecimal(percentualSeguro);
    }

    public BigDecimal seguro(BigDecimal subtotalProdutos) {
        return Dinheiro.percentual(subtotalProdutos, percentualSeguro);
    }
}
