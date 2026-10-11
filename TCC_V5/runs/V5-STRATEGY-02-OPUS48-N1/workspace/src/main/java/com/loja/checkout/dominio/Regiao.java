package com.loja.checkout.dominio;

import java.math.BigDecimal;

/**
 * Região do cliente. O enunciado diz que só a porcentagem do seguro muda e a
 * conta é a mesma em todas as regiões; por isso aqui cada caso é só um dado
 * (a alíquota), e o cálculo do seguro vive num método único.
 */
public enum Regiao {

    SUDESTE("0.01"),
    SUL("0.01"),
    CENTRO_OESTE("0.015"),
    NORTE("0.025"),
    NORDESTE("0.02");

    private final BigDecimal aliquotaSeguro;

    Regiao(String aliquotaSeguro) {
        this.aliquotaSeguro = new BigDecimal(aliquotaSeguro);
    }

    /** Seguro: porcentagem sobre o valor dos produtos, sem desconto e sem frete. */
    public BigDecimal seguro(BigDecimal subtotalProdutos) {
        return Dinheiro.centavos(subtotalProdutos.multiply(aliquotaSeguro));
    }
}
