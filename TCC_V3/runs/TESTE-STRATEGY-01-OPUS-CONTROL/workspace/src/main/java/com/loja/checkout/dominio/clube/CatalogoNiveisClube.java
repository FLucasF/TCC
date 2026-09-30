package com.loja.checkout.dominio.clube;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Component;

/** Reune os niveis do clube existentes. */
@Component
public class CatalogoNiveisClube {

    private final Map<String, NivelClube> porCodigo = new LinkedHashMap<>();

    public CatalogoNiveisClube(List<NivelClube> niveis) {
        niveis.forEach(nivel -> porCodigo.put(nivel.codigo(), nivel));
    }

    public Optional<NivelClube> buscar(String codigo) {
        return codigo == null ? Optional.empty() : Optional.ofNullable(porCodigo.get(codigo));
    }
}
