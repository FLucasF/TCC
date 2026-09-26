package com.loja.model;

public enum Cupom {
    BEMVINDO10("BEMVINDO10", TipoCupom.PERCENTUAL_PRODUTOS),
    MENOS50("MENOS50", TipoCupom.VALOR_FIXO_PRODUTOS),
    FRETEGRATIS("FRETEGRATIS", TipoCupom.FRETE_GRATIS),
    LEVE3PAGUE2("LEVE3PAGUE2", TipoCupom.LEVE3_PAGUE2);

    private final String codigo;
    private final TipoCupom tipo;

    Cupom(String codigo, TipoCupom tipo) {
        this.codigo = codigo;
        this.tipo = tipo;
    }

    public String getCodigo() {
        return codigo;
    }

    public TipoCupom getTipo() {
        return tipo;
    }

    public static Cupom fromCodigo(String codigo) {
        if (codigo == null) {
            return null;
        }
        for (Cupom cupom : values()) {
            if (cupom.codigo.equals(codigo)) {
                return cupom;
            }
        }
        return null;
    }

    public enum TipoCupom {
        PERCENTUAL_PRODUTOS,
        VALOR_FIXO_PRODUTOS,
        FRETE_GRATIS,
        LEVE3_PAGUE2
    }

}
