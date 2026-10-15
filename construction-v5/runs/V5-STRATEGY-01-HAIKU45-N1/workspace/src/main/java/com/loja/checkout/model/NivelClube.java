package com.loja.checkout.model;

public enum NivelClube {
    BRONZE(0.0, false, false),
    PRATA(0.02, false, false),
    OURO(0.05, true, true);

    private final double percentualCredito;
    private final boolean isentoFrete;
    private final boolean temBrinde;

    NivelClube(double percentualCredito, boolean isentoFrete, boolean temBrinde) {
        this.percentualCredito = percentualCredito;
        this.isentoFrete = isentoFrete;
        this.temBrinde = temBrinde;
    }

    public double getPercentualCredito() {
        return percentualCredito;
    }

    public boolean isIsentoFrete() {
        return isentoFrete;
    }

    public boolean isTemBrinde() {
        return temBrinde;
    }

    public static NivelClube de(String nome) {
        if (nome == null) {
            return null;
        }
        try {
            return NivelClube.valueOf(nome.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
