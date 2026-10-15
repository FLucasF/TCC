package com.loja.checkout.domain.cupom;

import java.util.HashMap;
import java.util.Map;

public class CupomFactory {

    private static final Map<String, Cupom> CUPONS = new HashMap<>();

    static {
        CUPONS.put("BEMVINDO10", new BemVindo10());
        CUPONS.put("MENOS50", new Menos50());
        CUPONS.put("FRETEGRATIS", new FreteGratis());
        CUPONS.put("LEVE3PAGUE2", new Leve3Pague2());
    }

    public static Cupom criar(String codigo) {
        return CUPONS.get(codigo);
    }

    public static boolean existe(String codigo) {
        return CUPONS.containsKey(codigo);
    }
}
