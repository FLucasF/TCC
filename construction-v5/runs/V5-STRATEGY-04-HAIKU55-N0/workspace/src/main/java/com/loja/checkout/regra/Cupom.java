package com.loja.checkout.regra;

public enum Cupom {
    BEMVINDO10,
    MENOS50,
    FRETEGRATIS,
    LEVE3PAGUE2;

    static Cupom de(String valor) {
        return ErroCheckout.paraEnum(Cupom.class, valor, CodigoErro.CUPOM_INVALIDO);
    }
}
