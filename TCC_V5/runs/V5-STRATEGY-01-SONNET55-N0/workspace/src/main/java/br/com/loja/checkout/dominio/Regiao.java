package br.com.loja.checkout.dominio;

import java.math.BigDecimal;

/** Muda apenas a porcentagem do seguro; a conta e a mesma para todas as regioes. */
public enum Regiao {
    SUDESTE("0.01"),
    SUL("0.01"),
    CENTRO_OESTE("0.015"),
    NORTE("0.025"),
    NORDESTE("0.02");

    private final BigDecimal taxaSeguro;

    Regiao(String taxaSeguro) {
        this.taxaSeguro = new BigDecimal(taxaSeguro);
    }

    public BigDecimal seguro(BigDecimal subtotalProdutos) {
        return Dinheiro.arredondar(subtotalProdutos.multiply(taxaSeguro));
    }

    public static Regiao de(String codigo) {
        try {
            return codigo == null ? null : valueOf(codigo);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
