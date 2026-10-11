package com.loja.checkout.service.cupom;

import com.loja.checkout.model.Cupom;

public class FactoryCupom {
    public static EstrategiaCupom criar(Cupom cupom) {
        return switch (cupom) {
            case BEMVINDO10 -> new EstrategiaCupom.Bemvindo10();
            case MENOS50 -> new EstrategiaCupom.Menos50();
            case FRETEGRATIS -> new EstrategiaCupom.FreteGratis();
            case LEVE3PAGUE2 -> new EstrategiaCupom.Leve3Pague2();
        };
    }
}
