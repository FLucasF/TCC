package com.loja.checkout.calculo;

import java.math.BigDecimal;

public enum ModalidadeEntrega {

    ECONOMICA(new BigDecimal("12.00"), new BigDecimal("2.00"), 7, null),
    EXPRESSA(new BigDecimal("25.00"), new BigDecimal("4.50"), 2, null),
    RETIRADA_LOJA(BigDecimal.ZERO, BigDecimal.ZERO, 1, null),
    MOTOBOY(new BigDecimal("18.00"), BigDecimal.ZERO, 0, new BigDecimal("5"));

    private final BigDecimal taxaFixa;
    private final BigDecimal taxaPorKg;
    private final int prazoDias;
    private final BigDecimal pesoMaximoKg;

    ModalidadeEntrega(BigDecimal taxaFixa, BigDecimal taxaPorKg, int prazoDias, BigDecimal pesoMaximoKg) {
        this.taxaFixa = taxaFixa;
        this.taxaPorKg = taxaPorKg;
        this.prazoDias = prazoDias;
        this.pesoMaximoKg = pesoMaximoKg;
    }

    public boolean disponivelPara(BigDecimal pesoKg) {
        return pesoMaximoKg == null || pesoKg.compareTo(pesoMaximoKg) <= 0;
    }

    public BigDecimal frete(BigDecimal pesoKg) {
        return taxaFixa.add(taxaPorKg.multiply(pesoKg));
    }

    public int prazoDias() {
        return prazoDias;
    }
}
