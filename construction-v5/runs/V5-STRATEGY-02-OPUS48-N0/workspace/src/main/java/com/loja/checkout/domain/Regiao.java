package com.loja.checkout.domain;

import java.math.BigDecimal;

/**
 * Regiao do cliente. O que muda entre uma regiao e outra e so a porcentagem do
 * seguro; a conta e sempre a mesma (porcentagem sobre o valor dos produtos).
 * Para criar uma regiao nova, basta adicionar uma constante aqui.
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

    public BigDecimal percentualSeguro() {
        return percentualSeguro;
    }

    /** Calcula o seguro sobre o valor dos produtos (sem desconto e sem frete). */
    public BigDecimal calcularSeguro(BigDecimal subtotalProdutos) {
        return Dinheiro.centavos(subtotalProdutos.multiply(percentualSeguro));
    }

    /** Converte o texto recebido do site; lanca REGIAO_INVALIDA se nao existir. */
    public static Regiao de(String valor) {
        if (valor != null) {
            for (Regiao regiao : values()) {
                if (regiao.name().equals(valor)) {
                    return regiao;
                }
            }
        }
        throw new CheckoutException(CodigoErro.REGIAO_INVALIDA);
    }
}
