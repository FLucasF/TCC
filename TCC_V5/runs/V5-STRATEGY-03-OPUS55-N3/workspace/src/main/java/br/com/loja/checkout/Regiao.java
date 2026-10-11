package br.com.loja.checkout;

import java.math.BigDecimal;

/** Só o percentual do seguro muda entre regiões; a conta é a mesma. */
public enum Regiao {
    SUDESTE("1"),
    SUL("1"),
    CENTRO_OESTE("1.5"),
    NORTE("2.5"),
    NORDESTE("2");

    private final String percentualSeguro;

    Regiao(String percentualSeguro) {
        this.percentualSeguro = percentualSeguro;
    }

    public BigDecimal seguro(BigDecimal subtotalProdutos) {
        return Dinheiro.percentual(subtotalProdutos, percentualSeguro);
    }
}
