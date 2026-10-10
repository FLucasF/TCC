package com.loja.checkout.dominio;

import com.loja.checkout.catalogo.Identificado;
import java.math.BigDecimal;

/**
 * Regiao do cliente. A conta do seguro e a mesma em todas as regioes,
 * so a aliquota muda, entao aqui basta o numero.
 */
public enum Regiao implements Identificado {

    SUDESTE("0.01"),
    SUL("0.01"),
    CENTRO_OESTE("0.015"),
    NORTE("0.025"),
    NORDESTE("0.02");

    private final BigDecimal aliquotaSeguro;

    Regiao(String aliquotaSeguro) {
        this.aliquotaSeguro = new BigDecimal(aliquotaSeguro);
    }

    @Override
    public String codigo() {
        return name();
    }

    public BigDecimal seguro(BigDecimal subtotalProdutos) {
        return Dinheiro.percentual(subtotalProdutos, aliquotaSeguro);
    }
}
