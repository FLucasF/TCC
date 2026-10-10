package com.loja.checkout;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

public class Catalogo<T extends Codificado> {
    private final Map<String, T> porCodigo;

    public Catalogo(List<T> opcoes) {
        this.porCodigo = opcoes.stream().collect(Collectors.toMap(Codificado::codigo, Function.identity()));
    }

    public Optional<T> buscar(String codigo) {
        return Optional.ofNullable(codigo).map(porCodigo::get);
    }
}
