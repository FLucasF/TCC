package com.loja.checkout.service;

import com.loja.checkout.domain.Cupom;
import com.loja.checkout.domain.cupom.*;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class CupomFactory {
    private final Map<String, Cupom> cupons = Map.of(
        "BEMVINDO10", new BemVindo10(),
        "MENOS50", new Menos50(),
        "FRETEGRATIS", new FreteGratis(),
        "LEVE3PAGUE2", new Leve3Pague2()
    );

    public Cupom obter(String codigo) {
        return cupons.get(codigo);
    }

    public boolean existe(String codigo) {
        return cupons.containsKey(codigo);
    }
}
