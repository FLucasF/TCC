package com.loja.roupas.checkout;

import java.math.BigDecimal;

public enum ModalidadeEntrega {
    ECONOMICA("ECONOMICA", new BigDecimal("12.00"), new BigDecimal("2.00"), 7),
    EXPRESSA("EXPRESSA", new BigDecimal("25.00"), new BigDecimal("4.50"), 2),
    RETIRADA_LOJA("RETIRADA_LOJA", BigDecimal.ZERO, BigDecimal.ZERO, 1),
    MOTOBOY("MOTOBOY", new BigDecimal("18.00"), BigDecimal.ZERO, 0, true);

    private final String codigo;
    private final BigDecimal tarifa;
    private final BigDecimal porKg;
    private final int prazo;
    private final boolean temLimiteKg;

    ModalidadeEntrega(String codigo, BigDecimal tarifa, BigDecimal porKg, int prazo) {
        this.codigo = codigo;
        this.tarifa = tarifa;
        this.porKg = porKg;
        this.prazo = prazo;
        this.temLimiteKg = false;
    }

    ModalidadeEntrega(String codigo, BigDecimal tarifa, BigDecimal porKg, int prazo, boolean temLimiteKg) {
        this.codigo = codigo;
        this.tarifa = tarifa;
        this.porKg = porKg;
        this.prazo = prazo;
        this.temLimiteKg = temLimiteKg;
    }

    public String getCodigo() {
        return codigo;
    }

    public BigDecimal getTarifa() {
        return tarifa;
    }

    public BigDecimal getPorKg() {
        return porKg;
    }

    public int getPrazo() {
        return prazo;
    }

    public boolean temLimiteKg() {
        return temLimiteKg;
    }

    public static ModalidadeEntrega fromCodigo(String codigo) {
        for (ModalidadeEntrega m : values()) {
            if (m.codigo.equals(codigo)) {
                return m;
            }
        }
        return null;
    }
}
