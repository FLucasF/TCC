package com.loja.checkout.calculo;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class Catalogo<T> {

    private final Map<String, T> itens;

    public Catalogo(List<T> lista, Function<T, String> codigo) {
        this.itens = lista.stream().collect(Collectors.toUnmodifiableMap(codigo, Function.identity()));
    }

    public Optional<T> buscar(String codigo) {
        return Optional.ofNullable(codigo).map(itens::get);
    }
}
