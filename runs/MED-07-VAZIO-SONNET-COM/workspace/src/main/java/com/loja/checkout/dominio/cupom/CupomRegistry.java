package com.loja.checkout.dominio.cupom;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

public class CupomRegistry {

    private final Map<String, Cupom> cupons;

    public CupomRegistry() {
        this(List.of(new Bemvindo10(), new Menos50(), new FreteGratis(), new Leve3Pague2()));
    }

    public CupomRegistry(List<Cupom> cupons) {
        this.cupons = cupons.stream()
                .collect(Collectors.toMap(Cupom::codigo, Function.identity()));
    }

    public Optional<Cupom> buscar(String codigo) {
        if (codigo == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(cupons.get(codigo));
    }
}
