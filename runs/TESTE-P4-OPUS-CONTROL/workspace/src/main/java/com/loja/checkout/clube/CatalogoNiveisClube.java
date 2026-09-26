package com.loja.checkout.clube;

import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Reune todos os niveis do clube. */
@Component
public class CatalogoNiveisClube {

    private final Map<String, NivelClube> porCodigo = new LinkedHashMap<>();

    public CatalogoNiveisClube(List<NivelClube> niveis) {
        niveis.forEach(n -> porCodigo.put(n.codigo(), n));
    }

    public Optional<NivelClube> buscar(String codigo) {
        return codigo == null ? Optional.empty() : Optional.ofNullable(porCodigo.get(codigo));
    }
}
