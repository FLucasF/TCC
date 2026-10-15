package com.loja.checkout.domain;

public enum NivelClube {
    BRONZE("BRONZE", 0.0, false),
    PRATA("PRATA", 0.02, false),
    OURO("OURO", 0.05, true);

    private final String codigo;
    private final double percentualCredito;
    private final boolean isencaoFrete;

    NivelClube(String codigo, double percentualCredito, boolean isencaoFrete) {
        this.codigo = codigo;
        this.percentualCredito = percentualCredito;
        this.isencaoFrete = isencaoFrete;
    }

    public String getCodigo() {
        return codigo;
    }

    public double getPercentualCredito() {
        return percentualCredito;
    }

    public boolean temIsencaoFrete() {
        return isencaoFrete;
    }

    public static NivelClube fromCodigo(String codigo) {
        if (codigo == null) {
            return null;
        }
        for (NivelClube n : values()) {
            if (n.codigo.equals(codigo)) {
                return n;
            }
        }
        return null;
    }
}
