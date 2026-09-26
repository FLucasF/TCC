package com.loja.checkout.modelo.cupom;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class RegistroCupons {
    private static final Map<String, Cupom> cupons = new HashMap<>();

    static {
        cupons.put("BEMVINDO10", new CupomBemVindo10());
        cupons.put("MENOS50", new CupomMenos50());
        cupons.put("FRETEGRATIS", new CupomFreteGratis());
        cupons.put("LEVE3PAGUE2", new CupomLeve3Pague2());
    }

    public static Optional<Cupom> obter(String codigo) {
        return Optional.ofNullable(cupons.get(codigo));
    }
}
