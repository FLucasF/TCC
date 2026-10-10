package com.loja.checkout.dominio.cupom;

import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Reune todos os cupons que valem hoje (ficam fixos no sistema). */
@Component
public class CatalogoCupons {

    private final Map<String, Cupom> porCodigo;

    public CatalogoCupons(List<Cupom> cupons) {
        this.porCodigo = cupons.stream().collect(Collectors.toMap(
                Cupom::codigo, Function.identity(), (a, b) -> a, LinkedHashMap::new));
    }

    public Optional<Cupom> buscar(String codigo) {
        return codigo == null ? Optional.empty() : Optional.ofNullable(porCodigo.get(codigo));
    }
}
