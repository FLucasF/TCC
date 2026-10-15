package com.loja.checkout.domain;

import java.math.BigDecimal;

/**
 * Nivel do cliente no clube da loja. Cada nivel define as vantagens:
 * credito de volta sobre os produtos, frete gratis, ate quantas parcelas
 * ficam sem juros e se ganha brinde acima de um certo valor em produtos.
 * Para criar um nivel novo, basta adicionar aqui com suas vantagens.
 */
public enum NivelClube {

    BRONZE(new BigDecimal("0.00"), false, 3, false),
    PRATA(new BigDecimal("0.02"), false, 3, false),
    OURO(new BigDecimal("0.05"), true, 6, true);

    /** Valor em produtos acima do qual o nivel que da brinde manda o brinde. */
    public static final BigDecimal VALOR_MINIMO_BRINDE = new BigDecimal("500.00");

    private final BigDecimal percentualCredito;
    private final boolean freteGratis;
    private final int maxParcelasSemJuros;
    private final boolean ganhaBrinde;

    NivelClube(BigDecimal percentualCredito, boolean freteGratis, int maxParcelasSemJuros, boolean ganhaBrinde) {
        this.percentualCredito = percentualCredito;
        this.freteGratis = freteGratis;
        this.maxParcelasSemJuros = maxParcelasSemJuros;
        this.ganhaBrinde = ganhaBrinde;
    }

    public BigDecimal percentualCredito() {
        return percentualCredito;
    }

    public boolean freteGratis() {
        return freteGratis;
    }

    public int maxParcelasSemJuros() {
        return maxParcelasSemJuros;
    }

    /** True quando o nivel manda brinde para este valor de produtos. */
    public boolean temBrinde(BigDecimal subtotalProdutos) {
        return ganhaBrinde && subtotalProdutos.compareTo(VALOR_MINIMO_BRINDE) > 0;
    }
}
