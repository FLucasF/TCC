package com.loja.checkout.dominio;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

public class Registro<T> {

    private final Map<String, T> porCodigo;
    private final Supplier<RuntimeException> erroNaoEncontrado;

    public Registro(List<T> casos, Function<T, String> codigo, Supplier<RuntimeException> erroNaoEncontrado) {
        this.porCodigo = casos.stream().collect(java.util.stream.Collectors.toMap(codigo, c -> c));
        this.erroNaoEncontrado = erroNaoEncontrado;
    }

    public T buscar(String codigo) {
        if (codigo == null || !porCodigo.containsKey(codigo)) {
            throw erroNaoEncontrado.get();
        }
        return porCodigo.get(codigo);
    }
}
