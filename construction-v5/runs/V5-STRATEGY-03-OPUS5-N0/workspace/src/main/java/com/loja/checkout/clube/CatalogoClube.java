package com.loja.checkout.clube;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Component;

/** Reune os niveis do clube da loja. */
@Component
public class CatalogoClube {

    private final Map<String, NivelClube> porCodigo = new LinkedHashMap<>();

    public CatalogoClube(List<NivelClube> niveis) {
        niveis.forEach(nivel -> porCodigo.put(nivel.codigo(), nivel));
    }

    public Optional<NivelClube> porCodigo(String codigo) {
        return Optional.ofNullable(codigo).map(porCodigo::get);
    }
}
