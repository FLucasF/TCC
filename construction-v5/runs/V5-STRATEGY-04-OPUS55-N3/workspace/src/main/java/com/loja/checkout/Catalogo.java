package com.loja.checkout;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Escolhe o caso pelo código: um mapa montado a partir das implementações registradas. */
public final class Catalogo<T extends Codificado> {

    private final Map<String, T> porCodigo;

    public Catalogo(List<T> casos) {
        this.porCodigo = casos.stream().collect(Collectors.toUnmodifiableMap(Codificado::codigo, Function.identity()));
    }

    public Optional<T> buscar(String codigo) {
        return codigo == null ? Optional.empty() : Optional.ofNullable(porCodigo.get(codigo));
    }
}
