package com.loja.checkout.resumo;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Optional;

/**
 * Região do cliente. O seguro é a mesma conta em todas as regiões — uma
 * porcentagem sobre o valor dos produtos —, só a porcentagem muda.
 */
public enum Regiao {

    SUDESTE("0.01"),
    SUL("0.01"),
    CENTRO_OESTE("0.015"),
    NORTE("0.025"),
    NORDESTE("0.02");

    private final BigDecimal percentualSeguro;

    Regiao(String percentualSeguro) {
        this.percentualSeguro = new BigDecimal(percentualSeguro);
    }

    public static Optional<Regiao> porCodigo(String codigo) {
        return Arrays.stream(values()).filter(regiao -> regiao.name().equals(codigo)).findFirst();
    }

    public BigDecimal seguro(BigDecimal subtotalProdutos) {
        return Dinheiro.centavos(subtotalProdutos.multiply(percentualSeguro));
    }
}
