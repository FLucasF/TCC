package com.loja.checkout.dominio;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Optional;

/**
 * Regiao do cliente. Entre as regioes muda so a aliquota do seguro, por isso
 * aqui tem apenas o numero: a conta do seguro e a mesma para todas.
 */
public enum Regiao {

    SUDESTE("0.010"),
    SUL("0.010"),
    CENTRO_OESTE("0.015"),
    NORTE("0.025"),
    NORDESTE("0.020");

    private final BigDecimal aliquotaSeguro;

    Regiao(String aliquotaSeguro) {
        this.aliquotaSeguro = new BigDecimal(aliquotaSeguro);
    }

    /** Seguro contra extravio e roubo: a aliquota sobre o valor dos produtos. */
    public BigDecimal seguro(BigDecimal subtotalProdutos) {
        return Dinheiro.percentual(subtotalProdutos, aliquotaSeguro);
    }

    public static Optional<Regiao> porCodigo(String codigo) {
        return Arrays.stream(values())
                .filter(regiao -> regiao.name().equals(codigo))
                .findFirst();
    }
}
