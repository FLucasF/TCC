package com.loja.checkout.cupom;

import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Reune todos os cupons que valem hoje. */
@Component
public class CatalogoCupons {

    private final Map<String, Cupom> porCodigo = new LinkedHashMap<>();

    public CatalogoCupons(List<Cupom> cupons) {
        cupons.forEach(c -> porCodigo.put(c.codigo(), c));
    }

    public Optional<Cupom> buscar(String codigo) {
        return codigo == null ? Optional.empty() : Optional.ofNullable(porCodigo.get(codigo));
    }
}
