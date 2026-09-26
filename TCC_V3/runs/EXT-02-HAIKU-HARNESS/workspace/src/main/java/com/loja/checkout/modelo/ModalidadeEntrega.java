package com.loja.checkout.modelo;

import java.math.BigDecimal;

public enum ModalidadeEntrega {
    ECONOMICA("ECONOMICA", new BigDecimal("12.00"), new BigDecimal("2.00"), 7, 999),
    EXPRESSA("EXPRESSA", new BigDecimal("25.00"), new BigDecimal("4.50"), 2, 999),
    RETIRADA_LOJA("RETIRADA_LOJA", new BigDecimal("0"), new BigDecimal("0"), 1, 999),
    MOTOBOY("MOTOBOY", new BigDecimal("18.00"), new BigDecimal("0"), 0, 5);

    private final String codigo;
    private final BigDecimal custoFixo;
    private final BigDecimal custoParKg;
    private final int prazo;
    private final double pesoMaximoKg;

    ModalidadeEntrega(String codigo, BigDecimal custoFixo, BigDecimal custoParKg, int prazo, double pesoMaximoKg) {
        this.codigo = codigo;
        this.custoFixo = custoFixo;
        this.custoParKg = custoParKg;
        this.prazo = prazo;
        this.pesoMaximoKg = pesoMaximoKg;
    }

    public String getCodigo() {
        return codigo;
    }

    public BigDecimal getCustoFixo() {
        return custoFixo;
    }

    public BigDecimal getCustoParKg() {
        return custoParKg;
    }

    public int getPrazo() {
        return prazo;
    }

    public double getPesoMaximoKg() {
        return pesoMaximoKg;
    }

    public boolean podeUsarComPeso(BigDecimal pesoTotal) {
        return pesoTotal.doubleValue() <= pesoMaximoKg;
    }
}
