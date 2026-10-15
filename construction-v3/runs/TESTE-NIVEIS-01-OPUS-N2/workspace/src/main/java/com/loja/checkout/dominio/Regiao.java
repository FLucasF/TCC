package com.loja.checkout.dominio;

import java.math.BigDecimal;

/**
 * Regiao do cliente. Entre as regioes muda somente a aliquota do seguro de
 * envio; a conta e a mesma em todas: percentual sobre o valor dos produtos.
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

    public BigDecimal seguro(BigDecimal subtotalProdutos) {
        return Dinheiro.percentual(subtotalProdutos, aliquotaSeguro);
    }
}
