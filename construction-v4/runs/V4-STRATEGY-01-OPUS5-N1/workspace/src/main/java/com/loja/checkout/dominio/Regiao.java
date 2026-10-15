package com.loja.checkout.dominio;

import java.math.BigDecimal;

/**
 * Regiao do cliente. A conta do seguro e a mesma em todas as regioes
 * (percentual sobre o valor dos produtos); so a taxa muda.
 */
public enum Regiao {

    SUDESTE("0.010"),
    SUL("0.010"),
    CENTRO_OESTE("0.015"),
    NORTE("0.025"),
    NORDESTE("0.020");

    private final BigDecimal taxaSeguro;

    Regiao(String taxaSeguro) {
        this.taxaSeguro = new BigDecimal(taxaSeguro);
    }

    public BigDecimal seguro(BigDecimal subtotalProdutos) {
        return Dinheiro.percentual(subtotalProdutos, taxaSeguro);
    }
}
