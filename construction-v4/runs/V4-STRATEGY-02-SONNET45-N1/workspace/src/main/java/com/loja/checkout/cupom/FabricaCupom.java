package com.loja.checkout.cupom;

import com.loja.checkout.model.enums.CodigoCupom;
import java.util.Map;

public class FabricaCupom {
    private static final Map<CodigoCupom, Cupom> CUPONS = Map.of(
        CodigoCupom.BEMVINDO10, new CupomBemvindo10(),
        CodigoCupom.MENOS50, new CupomMenos50(),
        CodigoCupom.FRETEGRATIS, new CupomFreteGratis(),
        CodigoCupom.LEVE3PAGUE2, new CupomLeve3Pague2()
    );

    public static Cupom criar(CodigoCupom codigo) {
        return CUPONS.get(codigo);
    }
}
