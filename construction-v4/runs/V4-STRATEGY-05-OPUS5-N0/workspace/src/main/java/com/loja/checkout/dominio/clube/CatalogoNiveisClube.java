package com.loja.checkout.dominio.clube;

import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Reune todos os niveis do clube da loja. */
@Component
public class CatalogoNiveisClube {

    private final Map<String, NivelClube> porCodigo;

    public CatalogoNiveisClube(List<NivelClube> niveis) {
        this.porCodigo = niveis.stream().collect(Collectors.toMap(
                NivelClube::codigo, Function.identity(), (a, b) -> a, LinkedHashMap::new));
    }

    public Optional<NivelClube> buscar(String codigo) {
        return codigo == null ? Optional.empty() : Optional.ofNullable(porCodigo.get(codigo));
    }
}
