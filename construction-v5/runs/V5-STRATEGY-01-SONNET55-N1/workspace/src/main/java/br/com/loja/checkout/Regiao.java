package br.com.loja.checkout;

import java.math.BigDecimal;

/** Só a taxa do seguro muda de uma região para outra; a conta é a mesma. */
public enum Regiao {
    SUDESTE("0.01"),
    SUL("0.01"),
    CENTRO_OESTE("0.015"),
    NORTE("0.025"),
    NORDESTE("0.02");

    private final String taxaSeguro;

    Regiao(String taxaSeguro) {
        this.taxaSeguro = taxaSeguro;
    }

    public BigDecimal seguro(BigDecimal subtotalProdutos) {
        return Dinheiro.percentual(subtotalProdutos, taxaSeguro);
    }
}
