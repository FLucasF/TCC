package com.loja.checkout.domain.cupom;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class RegistroCupons {
    private static final Map<String, EstrategiaCupom> cupons = new HashMap<>();

    static {
        cupons.put("BEMVINDO10", new CupomBemvindo10());
        cupons.put("MENOS50", new CupomMenos50());
        cupons.put("FRETEGRATIS", new CupomFreteGratis());
        cupons.put("LEVE3PAGUE2", new CupomLeve3Pague2());
    }

    public static Optional<EstrategiaCupom> obter(String codigo) {
        return Optional.ofNullable(cupons.get(codigo));
    }

    public static boolean existe(String codigo) {
        return cupons.containsKey(codigo);
    }
}
