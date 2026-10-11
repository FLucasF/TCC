package com.loja.checkout.domain.clube;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Guarda os níveis do clube por código. Achar o nível é uma busca no mapa. */
@Component
public class RegistroNiveis {

    private final Map<String, NivelClube> porCodigo;

    public RegistroNiveis(List<NivelClube> niveis) {
        this.porCodigo = niveis.stream()
                .collect(Collectors.toUnmodifiableMap(NivelClube::codigo, Function.identity()));
    }

    public Optional<NivelClube> buscar(String codigo) {
        return Optional.ofNullable(codigo).map(porCodigo::get);
    }
}
