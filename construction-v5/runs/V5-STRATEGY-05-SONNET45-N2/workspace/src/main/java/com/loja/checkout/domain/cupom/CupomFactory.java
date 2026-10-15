package com.loja.checkout.domain.cupom;

import java.util.HashMap;
import java.util.Map;

public class CupomFactory {

    private static final Map<String, Cupom> cupons = new HashMap<>();

    static {
        cupons.put("BEMVINDO10", new BemVindo10());
        cupons.put("MENOS50", new Menos50());
        cupons.put("FRETEGRATIS", new FreteGratis());
        cupons.put("LEVE3PAGUE2", new Leve3Pague2());
    }

    public static Cupom obter(String codigo) {
        return cupons.get(codigo);
    }
}
