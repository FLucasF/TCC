package com.loja.checkout.dominio;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * Região do cliente. A conta do seguro é a mesma em todas as regiões:
 * muda só a porcentagem cobrada pela seguradora.
 */
public enum Regiao {

    SUDESTE("1"),
    SUL("1"),
    CENTRO_OESTE("1.5"),
    NORTE("2.5"),
    NORDESTE("2");

    private static final Catalogo<Regiao> CATALOGO = Catalogo.de(Regiao.class);

    private final BigDecimal percentualSeguro;

    Regiao(String percentualSeguro) {
        this.percentualSeguro = new BigDecimal(percentualSeguro);
    }

    public static Optional<Regiao> de(String codigo) {
        return CATALOGO.buscar(codigo);
    }

    public BigDecimal seguro(BigDecimal subtotalProdutos) {
        return Dinheiro.percentual(subtotalProdutos, percentualSeguro);
    }
}
