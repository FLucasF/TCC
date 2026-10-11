package com.loja.checkout.dominio.cupom;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;

@Component
public class Cupons {
    private final Map<String, Cupom> registro = Map.of(
        "BEMVINDO10",  new CupomBemVindo10(),
        "MENOS50",     new CupomMenos50(),
        "FRETEGRATIS", new CupomFreteGratis(),
        "LEVE3PAGUE2", new CupomLeve3Pague2()
    );

    public Optional<Cupom> encontrar(String codigo) {
        return Optional.ofNullable(registro.get(codigo));
    }
}
