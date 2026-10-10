package com.loja.checkout.dominio;

import java.math.BigDecimal;

/**
 * Regiões do cliente e o percentual de seguro cobrado pela seguradora.
 *
 * <p>A conta do seguro é a mesma em todas as regiões (percentual sobre o valor
 * dos produtos, sem desconto e sem frete). Só o percentual muda.
 */
public enum Regiao {

    SUDESTE(new BigDecimal("1")),
    SUL(new BigDecimal("1")),
    CENTRO_OESTE(new BigDecimal("1.5")),
    NORTE(new BigDecimal("2.5")),
    NORDESTE(new BigDecimal("2"));

    private final BigDecimal percentualSeguro;

    Regiao(BigDecimal percentualSeguro) {
        this.percentualSeguro = percentualSeguro;
    }

    /** Seguro do envio: percentual da região sobre o valor dos produtos. */
    public BigDecimal seguro(BigDecimal subtotalProdutos) {
        return Dinheiro.porcentagem(subtotalProdutos, percentualSeguro);
    }
}
