package com.loja.checkout.model;

import java.math.BigDecimal;

public enum FormaPagamento {
    PIX(1, 1, null, null),
    BOLETO(1, 1, new BigDecimal("3.49"), new BigDecimal("1000.00")),
    CARTAO(1, 12, null, null);

    private final int parcelasMin;
    private final int parcelasMax;
    private final BigDecimal tarifa;
    private final BigDecimal limiteMaximo;

    FormaPagamento(int parcelasMin, int parcelasMax, BigDecimal tarifa, BigDecimal limiteMaximo) {
        this.parcelasMin = parcelasMin;
        this.parcelasMax = parcelasMax;
        this.tarifa = tarifa;
        this.limiteMaximo = limiteMaximo;
    }

    public int getParcelasMin() {
        return parcelasMin;
    }

    public int getParcelasMax() {
        return parcelasMax;
    }

    public BigDecimal getTarifa() {
        return tarifa;
    }

    public BigDecimal getLimiteMaximo() {
        return limiteMaximo;
    }

    public boolean aceitaValor(BigDecimal valor) {
        return limiteMaximo == null || valor.compareTo(limiteMaximo) <= 0;
    }

    public boolean aceitaParcelas(int parcelas) {
        return parcelas >= parcelasMin && parcelas <= parcelasMax;
    }
}
