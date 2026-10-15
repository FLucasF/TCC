package com.loja.checkout.cupom;

import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class CupomRegistry {

    private final Map<String, Cupom> cupons = Map.of(
            "BEMVINDO10", new CupomBemVindo10(),
            "MENOS50", new CupomMenos50(),
            "FRETEGRATIS", new CupomFreteGratis(),
            "LEVE3PAGUE2", new CupomLeve3Pague2()
    );

    public Cupom buscar(String codigo) {
        return cupons.get(codigo);
    }
}
