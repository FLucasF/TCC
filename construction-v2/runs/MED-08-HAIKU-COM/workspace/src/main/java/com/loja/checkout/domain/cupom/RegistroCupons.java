package com.loja.checkout.domain.cupom;

import java.util.HashMap;
import java.util.Map;

public class RegistroCupons {
    private static final Map<String, Cupom> CUPONS = new HashMap<>();

    static {
        CUPONS.put("BEMVINDO10", new CupomBemVindo10());
        CUPONS.put("MENOS50", new CupomMenos50());
        CUPONS.put("FRETEGRATIS", new CupomFrteGratis());
        CUPONS.put("LEVE3PAGUE2", new CupomLeve3Pague2());
    }

    public static Cupom obter(String codigo) {
        return CUPONS.get(codigo);
    }

    public static boolean existe(String codigo) {
        return CUPONS.containsKey(codigo);
    }
}
