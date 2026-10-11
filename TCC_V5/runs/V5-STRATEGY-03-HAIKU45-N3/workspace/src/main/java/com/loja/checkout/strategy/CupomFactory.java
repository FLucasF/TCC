package com.loja.checkout.strategy;

import com.loja.checkout.enums.TipoCupom;

public class CupomFactory {

    public static CupomStrategy criar(TipoCupom tipoCupom) {
        return switch (tipoCupom) {
            case BEMVINDO10 -> new CupomBemVindo10();
            case MENOS50 -> new CupomMenos50();
            case FRETEGRATIS -> new CupomFreteGratis();
            case LEVE3PAGUE2 -> new CupomLeve3Pague2();
        };
    }
}
