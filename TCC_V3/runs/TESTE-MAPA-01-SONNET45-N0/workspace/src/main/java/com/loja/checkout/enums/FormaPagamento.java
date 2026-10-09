package com.loja.checkout.enums;

import java.math.BigDecimal;

public enum FormaPagamento {
    PIX(1, 1, new BigDecimal("0.05"), true, null, BigDecimal.ZERO),
    BOLETO(1, 1, BigDecimal.ZERO, false, new BigDecimal("1000.00"), new BigDecimal("3.49")),
    CARTAO(1, 12, BigDecimal.ZERO, false, null, BigDecimal.ZERO);

    private final int parcelasMin;
    private final int parcelasMax;
    private final BigDecimal percentualDesconto;
    private final boolean aplicaDescontoNoTotal;
    private final BigDecimal limiteMaximo;
    private final BigDecimal tarifa;

    FormaPagamento(int parcelasMin, int parcelasMax, BigDecimal percentualDesconto,
                   boolean aplicaDescontoNoTotal, BigDecimal limiteMaximo, BigDecimal tarifa) {
        this.parcelasMin = parcelasMin;
        this.parcelasMax = parcelasMax;
        this.percentualDesconto = percentualDesconto;
        this.aplicaDescontoNoTotal = aplicaDescontoNoTotal;
        this.limiteMaximo = limiteMaximo;
        this.tarifa = tarifa;
    }

    public int getParcelasMin() {
        return parcelasMin;
    }

    public int getParcelasMax() {
        return parcelasMax;
    }

    public BigDecimal getPercentualDesconto() {
        return percentualDesconto;
    }

    public boolean isAplicaDescontoNoTotal() {
        return aplicaDescontoNoTotal;
    }

    public BigDecimal getLimiteMaximo() {
        return limiteMaximo;
    }

    public BigDecimal getTarifa() {
        return tarifa;
    }
}
