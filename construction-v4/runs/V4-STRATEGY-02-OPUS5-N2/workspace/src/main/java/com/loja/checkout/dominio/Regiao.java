package com.loja.checkout.dominio;

import java.math.BigDecimal;

/**
 * A região do cliente. O seguro do envio é sempre a mesma conta — uma porcentagem
 * do valor dos produtos —, só a porcentagem muda de região para região.
 */
public enum Regiao implements Identificado {

    SUDESTE("1"),
    SUL("1"),
    CENTRO_OESTE("1.5"),
    NORTE("2.5"),
    NORDESTE("2");

    private final BigDecimal percentualSeguro;

    Regiao(String percentualSeguro) {
        this.percentualSeguro = new BigDecimal(percentualSeguro);
    }

    @Override
    public String codigo() {
        return name();
    }

    public BigDecimal seguro(BigDecimal subtotalProdutos) {
        return Dinheiro.percentual(subtotalProdutos, percentualSeguro);
    }
}
