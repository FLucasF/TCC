package com.loja.checkout.estrategia;

import com.loja.checkout.domain.Cupom;

public class FabricaCupom {
    public static EstrategiaCupom criar(Cupom cupom) {
        if (cupom == null) {
            return new CupomNenhum();
        }
        return switch (cupom) {
            case BEMVINDO10 -> new CupomBemvindo10();
            case MENOS50 -> new CupomMenos50();
            case FRETEGRATIS -> new CupomFreteGratis();
            case LEVE3PAGUE2 -> new CupomLeve3Pague2();
        };
    }
}
