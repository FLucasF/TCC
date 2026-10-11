package com.loja.checkout.regra;

import java.math.BigDecimal;

public enum Modalidade {

    ECONOMICA("12.00", "2.00", 7, null),
    EXPRESSA("25.00", "4.50", 2, null),
    RETIRADA_LOJA("0.00", "0.00", 1, null),
    MOTOBOY("18.00", "0.00", 0, "5");

    private final BigDecimal taxaFixa;
    private final BigDecimal taxaPorKg;
    private final int prazoDias;
    private final BigDecimal pesoMaximoKg;

    Modalidade(String taxaFixa, String taxaPorKg, int prazoDias, String pesoMaximoKg) {
        this.taxaFixa = new BigDecimal(taxaFixa);
        this.taxaPorKg = new BigDecimal(taxaPorKg);
        this.prazoDias = prazoDias;
        this.pesoMaximoKg = pesoMaximoKg == null ? null : new BigDecimal(pesoMaximoKg);
    }

    public BigDecimal frete(BigDecimal pesoKg) {
        return taxaFixa.add(taxaPorKg.multiply(pesoKg));
    }

    public int prazoDias() {
        return prazoDias;
    }

    public boolean atende(BigDecimal pesoKg) {
        return pesoMaximoKg == null || pesoKg.compareTo(pesoMaximoKg) <= 0;
    }

    static Modalidade de(String valor) {
        return ErroCheckout.paraEnum(Modalidade.class, valor, CodigoErro.MODALIDADE_INVALIDA);
    }
}
