package com.loja.cupom;

import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Component;

@Component
public class CupomRegistry {
    private final Map<String, Cupom> cupons;

    public CupomRegistry() {
        cupons = new HashMap<>();
        cupons.put("BEMVINDO10", new Bemvindo10());
        cupons.put("MENOS50", new Menos50());
        cupons.put("FRETEGRATIS", new FretegratisCupom());
        cupons.put("LEVE3PAGUE2", new Leve3pague2());
    }

    public Cupom get(String code) {
        return cupons.get(code);
    }
}
