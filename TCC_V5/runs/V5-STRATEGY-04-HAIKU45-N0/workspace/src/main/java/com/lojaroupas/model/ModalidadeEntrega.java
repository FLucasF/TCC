package com.lojaroupas.model;

import java.math.BigDecimal;

public enum ModalidadeEntrega {
    ECONOMICA("ECONOMICA", new BigDecimal("12.00"), new BigDecimal("2.00"), 7),
    EXPRESSA("EXPRESSA", new BigDecimal("25.00"), new BigDecimal("4.50"), 2),
    RETIRADA_LOJA("RETIRADA_LOJA", BigDecimal.ZERO, BigDecimal.ZERO, 1),
    MOTOBOY("MOTOBOY", new BigDecimal("18.00"), BigDecimal.ZERO, 0);

    private final String codigo;
    private final BigDecimal valorFixo;
    private final BigDecimal valorPorKg;
    private final Integer prazoEntregaDias;

    ModalidadeEntrega(String codigo, BigDecimal valorFixo, BigDecimal valorPorKg, Integer prazoEntregaDias) {
        this.codigo = codigo;
        this.valorFixo = valorFixo;
        this.valorPorKg = valorPorKg;
        this.prazoEntregaDias = prazoEntregaDias;
    }

    public static ModalidadeEntrega fromCodigo(String codigo) {
        for (ModalidadeEntrega m : values()) {
            if (m.codigo.equals(codigo)) {
                return m;
            }
        }
        return null;
    }

    public String getCodigo() {
        return codigo;
    }

    public BigDecimal getValorFixo() {
        return valorFixo;
    }

    public BigDecimal getValorPorKg() {
        return valorPorKg;
    }

    public Integer getPrazoEntregaDias() {
        return prazoEntregaDias;
    }

}
