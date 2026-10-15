package br.com.loja.checkout.dominio;

import java.math.BigDecimal;

/** Região do cliente e a porcentagem de seguro cobrada nela. */
public enum Regiao {
    SUDESTE("1"),
    SUL("1"),
    CENTRO_OESTE("1.5"),
    NORTE("2.5"),
    NORDESTE("2");

    private final BigDecimal percentualSeguro;

    Regiao(String percentualSeguro) {
        this.percentualSeguro = new BigDecimal(percentualSeguro);
    }

    public BigDecimal seguro(BigDecimal subtotal) {
        return Dinheiro.percentual(subtotal, percentualSeguro);
    }
}
