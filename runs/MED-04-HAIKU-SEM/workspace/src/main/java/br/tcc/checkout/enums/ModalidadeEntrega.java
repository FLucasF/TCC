package br.tcc.checkout.enums;

import java.math.BigDecimal;

public enum ModalidadeEntrega {
    ECONOMICA("ECONOMICA", BigDecimal.valueOf(12.00), BigDecimal.valueOf(2.00), 7),
    EXPRESSA("EXPRESSA", BigDecimal.valueOf(25.00), BigDecimal.valueOf(4.50), 2),
    RETIRADA_LOJA("RETIRADA_LOJA", BigDecimal.ZERO, BigDecimal.ZERO, 1),
    MOTOBOY("MOTOBOY", BigDecimal.valueOf(18.00), BigDecimal.ZERO, 0);

    private final String codigo;
    private final BigDecimal valorBase;
    private final BigDecimal valorPorKg;
    private final int prazoEntregaDias;

    ModalidadeEntrega(String codigo, BigDecimal valorBase, BigDecimal valorPorKg, int prazoEntregaDias) {
        this.codigo = codigo;
        this.valorBase = valorBase;
        this.valorPorKg = valorPorKg;
        this.prazoEntregaDias = prazoEntregaDias;
    }

    public String getCodigo() {
        return codigo;
    }

    public BigDecimal getValorBase() {
        return valorBase;
    }

    public BigDecimal getValorPorKg() {
        return valorPorKg;
    }

    public int getPrazoEntregaDias() {
        return prazoEntregaDias;
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
