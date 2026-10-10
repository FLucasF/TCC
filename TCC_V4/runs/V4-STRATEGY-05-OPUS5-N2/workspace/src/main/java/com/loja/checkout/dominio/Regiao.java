package com.loja.checkout.dominio;

import java.math.BigDecimal;

/**
 * Regiao do cliente. Todo pedido vai com seguro contra extravio e roubo: a conta e a
 * mesma em todas as regioes, so o percentual cobrado pela seguradora muda.
 */
public enum Regiao {

    SUDESTE("1"),
    SUL("1"),
    CENTRO_OESTE("1.5"),
    NORTE("2.5"),
    NORDESTE("2");

    public static final Catalogo<Regiao> CATALOGO = Catalogo.de(Erro.REGIAO_INVALIDA, values());

    private final BigDecimal percentualSeguro;

    Regiao(String percentualSeguro) {
        this.percentualSeguro = new BigDecimal(percentualSeguro);
    }

    /** Percentual sobre o valor dos produtos, sem desconto e sem frete. */
    public BigDecimal seguro(BigDecimal subtotalProdutos) {
        return Dinheiro.percentual(percentualSeguro, subtotalProdutos);
    }
}
