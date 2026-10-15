package com.lojaroupas.model;

import java.math.BigDecimal;

public enum NivelClube {
    BRONZE("BRONZE", BigDecimal.ZERO, false),
    PRATA("PRATA", new BigDecimal("0.02"), false),
    OURO("OURO", new BigDecimal("0.05"), true);

    private final String codigo;
    private final BigDecimal percentualCreditoProdutos;
    private final boolean contemFreteGratis;

    NivelClube(String codigo, BigDecimal percentualCreditoProdutos, boolean contemFreteGratis) {
        this.codigo = codigo;
        this.percentualCreditoProdutos = percentualCreditoProdutos;
        this.contemFreteGratis = contemFreteGratis;
    }

    public static NivelClube fromCodigo(String codigo) {
        for (NivelClube n : values()) {
            if (n.codigo.equals(codigo)) {
                return n;
            }
        }
        return null;
    }

    public String getCodigo() {
        return codigo;
    }

    public BigDecimal getPercentualCreditoProdutos() {
        return percentualCreditoProdutos;
    }

    public boolean isContemFreteGratis() {
        return contemFreteGratis;
    }

}
