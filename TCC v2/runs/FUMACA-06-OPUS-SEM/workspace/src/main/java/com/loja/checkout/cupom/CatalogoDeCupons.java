package com.loja.checkout.cupom;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class CatalogoDeCupons {

    private final Map<String, Cupom> porCodigo;

    public CatalogoDeCupons(List<Cupom> cupons) {
        this.porCodigo = cupons.stream()
                .collect(Collectors.toUnmodifiableMap(Cupom::codigo, Function.identity()));
    }

    public Optional<Cupom> buscar(String codigo) {
        return codigo == null ? Optional.empty() : Optional.ofNullable(porCodigo.get(codigo));
    }
}
