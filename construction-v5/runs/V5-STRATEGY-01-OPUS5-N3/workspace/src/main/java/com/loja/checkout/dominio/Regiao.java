package com.loja.checkout.dominio;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Região do cliente. O seguro é a mesma conta em todas — porcentagem sobre o
 * valor dos produtos — então aqui só muda a alíquota.
 */
public enum Regiao {

    SUDESTE("0.01"),
    SUL("0.01"),
    CENTRO_OESTE("0.015"),
    NORTE("0.025"),
    NORDESTE("0.02");

    public static final Catalogo<Regiao> CATALOGO = new Catalogo<>(
            Arrays.stream(values()).collect(Collectors.toMap(Enum::name, r -> r)),
            "REGIAO_INVALIDA");

    private final BigDecimal aliquotaSeguro;

    Regiao(String aliquotaSeguro) {
        this.aliquotaSeguro = new BigDecimal(aliquotaSeguro);
    }

    public BigDecimal seguro(BigDecimal subtotalProdutos) {
        return Dinheiro.percentual(subtotalProdutos, aliquotaSeguro);
    }
}
