package com.loja.checkout.domain;

import java.math.BigDecimal;

/**
 * Regiões do cliente e o percentual que a seguradora cobra pelo seguro do envio.
 *
 * <p>É só a porcentagem que muda: o seguro é sempre essa porcentagem sobre o valor
 * dos produtos (sem desconto e sem frete), arredondada para centavos.
 */
public enum Regiao {

    SUDESTE(new BigDecimal("0.01")),
    SUL(new BigDecimal("0.01")),
    CENTRO_OESTE(new BigDecimal("0.015")),
    NORTE(new BigDecimal("0.025")),
    NORDESTE(new BigDecimal("0.02"));

    private final BigDecimal percentualSeguro;

    Regiao(BigDecimal percentualSeguro) {
        this.percentualSeguro = percentualSeguro;
    }

    /** Seguro sobre o valor dos produtos, arredondado para centavos. */
    public BigDecimal calcularSeguro(BigDecimal subtotalProdutos) {
        return Dinheiro.percentual(subtotalProdutos, percentualSeguro);
    }
}
