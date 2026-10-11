package com.loja.checkout.dominio;

import java.math.BigDecimal;

/**
 * Regiao do cliente. A conta do seguro e a mesma em todas (a porcentagem sobre
 * o valor dos produtos); so a porcentagem muda. Por isso aqui e so dado: um
 * numero por regiao, nao um comportamento diferente por caso.
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

    /** Seguro bruto do envio; quem chama arredonda para centavos. */
    public BigDecimal seguro(BigDecimal subtotalProdutos) {
        return subtotalProdutos.multiply(percentualSeguro);
    }
}
