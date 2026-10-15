package com.loja.checkout.cupom;

import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Reune todos os cupons validos hoje. */
@Component
public class CatalogoCupons {

    private final Map<String, Cupom> porCodigo;

    public CatalogoCupons(List<Cupom> cupons) {
        Map<String, Cupom> mapa = new LinkedHashMap<>();
        cupons.forEach(c -> mapa.put(c.codigo(), c));
        this.porCodigo = Map.copyOf(mapa);
    }

    public Optional<Cupom> buscar(String codigo) {
        if (codigo == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(porCodigo.get(codigo));
    }
}
