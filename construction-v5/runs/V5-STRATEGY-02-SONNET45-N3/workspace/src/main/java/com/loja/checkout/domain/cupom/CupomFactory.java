package com.loja.checkout.domain.cupom;

import java.util.Map;

public class CupomFactory {

    private static final Map<String, Cupom> CUPONS = Map.of(
        "BEMVINDO10", new CupomBemVindo10(),
        "MENOS50", new CupomMenos50(),
        "FRETEGRATIS", new CupomFreteGratis(),
        "LEVE3PAGUE2", new CupomLeve3Pague2()
    );

    public static Cupom obter(String codigo) {
        return CUPONS.get(codigo);
    }

    public static boolean existe(String codigo) {
        return CUPONS.containsKey(codigo);
    }
}
