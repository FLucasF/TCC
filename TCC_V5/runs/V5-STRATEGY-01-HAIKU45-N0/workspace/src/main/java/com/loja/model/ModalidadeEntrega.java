package com.loja.model;

public enum ModalidadeEntrega {
    ECONOMICA("ECONOMICA", 12.00, 2.00, 7),
    EXPRESSA("EXPRESSA", 25.00, 4.50, 2),
    RETIRADA_LOJA("RETIRADA_LOJA", 0.00, 0.00, 1),
    MOTOBOY("MOTOBOY", 18.00, 0.00, 0);

    private final String codigo;
    private final double baseFixa;
    private final double porKg;
    private final int prazoEntregaDias;

    ModalidadeEntrega(String codigo, double baseFixa, double porKg, int prazoEntregaDias) {
        this.codigo = codigo;
        this.baseFixa = baseFixa;
        this.porKg = porKg;
        this.prazoEntregaDias = prazoEntregaDias;
    }

    public String getCodigo() {
        return codigo;
    }

    public double getBaseFixa() {
        return baseFixa;
    }

    public double getPorKg() {
        return porKg;
    }

    public int getPrazoEntregaDias() {
        return prazoEntregaDias;
    }

    public static ModalidadeEntrega fromCodigo(String codigo) {
        for (ModalidadeEntrega m : ModalidadeEntrega.values()) {
            if (m.codigo.equals(codigo)) {
                return m;
            }
        }
        return null;
    }
}
