package com.loja.checkout.domain.cupom;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;

@Component
public class RegistroCupons {

    private final Map<String, Cupom> cupons;

    public RegistroCupons() {
        this.cupons = Map.of(
                "BEMVINDO10", new Bemvindo10(),
                "MENOS50", new Menos50(),
                "FRETEGRATIS", new FreteGratis(),
                "LEVE3PAGUE2", new Leve3Pague2()
        );
    }

    public Optional<Cupom> buscar(String codigo) {
        return Optional.ofNullable(cupons.get(codigo));
    }
}
