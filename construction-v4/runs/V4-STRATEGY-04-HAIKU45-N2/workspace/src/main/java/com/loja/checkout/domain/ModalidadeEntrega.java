package com.loja.checkout.domain;

public enum ModalidadeEntrega {
    ECONOMICA("ECONOMICA", 12.00, 2.00, 7),
    EXPRESSA("EXPRESSA", 25.00, 4.50, 2),
    RETIRADA_LOJA("RETIRADA_LOJA", 0.00, 0.00, 1),
    MOTOBOY("MOTOBOY", 18.00, 0.00, 0);

    private final String codigo;
    private final double custoFixo;
    private final double custoPorKg;
    private final int prazoDias;

    ModalidadeEntrega(String codigo, double custoFixo, double custoPorKg, int prazoDias) {
        this.codigo = codigo;
        this.custoFixo = custoFixo;
        this.custoPorKg = custoPorKg;
        this.prazoDias = prazoDias;
    }

    public String getCodigo() {
        return codigo;
    }

    public double getCustoFixo() {
        return custoFixo;
    }

    public double getCustoPorKg() {
        return custoPorKg;
    }

    public int getPrazoDias() {
        return prazoDias;
    }

    public static ModalidadeEntrega fromCodigo(String codigo) {
        if (codigo == null) {
            return null;
        }
        for (ModalidadeEntrega m : values()) {
            if (m.codigo.equals(codigo)) {
                return m;
            }
        }
        return null;
    }

    public boolean temLimiteKg() {
        return this == MOTOBOY;
    }

    public int getLimiteKg() {
        return 5;
    }
}
