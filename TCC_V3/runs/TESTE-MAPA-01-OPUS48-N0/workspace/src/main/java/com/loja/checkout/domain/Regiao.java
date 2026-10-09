package com.loja.checkout.domain;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * Região do cliente. A conta do seguro é a mesma em todas: a porcentagem
 * sobre o valor dos produtos (sem desconto e sem frete). Só a porcentagem muda.
 */
public enum Regiao {

    SUDESTE(0.01),
    SUL(0.01),
    CENTRO_OESTE(0.015),
    NORTE(0.025),
    NORDESTE(0.02);

    private final BigDecimal percentualSeguro;

    Regiao(double percentualSeguro) {
        this.percentualSeguro = BigDecimal.valueOf(percentualSeguro);
    }

    public static Optional<Regiao> fromCodigo(String codigo) {
        return Enums.fromNome(Regiao.class, codigo);
    }

    public BigDecimal seguro(BigDecimal subtotalProdutos) {
        return Dinheiro.arredondar(subtotalProdutos.multiply(percentualSeguro));
    }
}
