package com.loja.checkout.dominio;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Resolve um código no caso correspondente, sem encadear condições. */
public final class Catalogo<T extends Codificavel> {

    private final Map<String, T> porCodigo;

    public Catalogo(List<T> casos) {
        this.porCodigo = casos.stream()
                .collect(Collectors.toMap(Codificavel::codigo, Function.identity(), (a, b) -> a, LinkedHashMap::new));
    }

    public Optional<T> buscar(String codigo) {
        return Optional.ofNullable(codigo).map(porCodigo::get);
    }
}
