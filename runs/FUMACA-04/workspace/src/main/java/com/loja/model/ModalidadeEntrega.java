package com.loja.model;

import java.math.BigDecimal;

public enum ModalidadeEntrega {
    ECONOMICA("ECONOMICA", new BigDecimal("12.00"), new BigDecimal("2.00"), 7),
    EXPRESSA("EXPRESSA", new BigDecimal("25.00"), new BigDecimal("4.50"), 2),
    RETIRADA_LOJA("RETIRADA_LOJA", new BigDecimal("0.00"), new BigDecimal("0.00"), 1),
    MOTOBOY("MOTOBOY", new BigDecimal("18.00"), new BigDecimal("0.00"), 0);

    private final String codigo;
    private final BigDecimal taxaBase;
    private final BigDecimal taxaPorKg;
    private final Integer prazo;

    ModalidadeEntrega(String codigo, BigDecimal taxaBase, BigDecimal taxaPorKg, Integer prazo) {
        this.codigo = codigo;
        this.taxaBase = taxaBase;
        this.taxaPorKg = taxaPorKg;
        this.prazo = prazo;
    }

    public String getCodigo() {
        return codigo;
    }

    public BigDecimal getTaxaBase() {
        return taxaBase;
    }

    public BigDecimal getTaxaPorKg() {
        return taxaPorKg;
    }

    public Integer getPrazo() {
        return prazo;
    }

    public static ModalidadeEntrega fromCodigo(String codigo) {
        if (codigo == null) {
            return null;
        }
        for (ModalidadeEntrega modalidade : values()) {
            if (modalidade.codigo.equals(codigo)) {
                return modalidade;
            }
        }
        return null;
    }

}
