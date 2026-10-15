package com.loja.resumo;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

public class Catalogo<T extends Codigado> {

    private final Map<String, T> porCodigo;

    public Catalogo(List<T> itens) {
        this.porCodigo = itens.stream().collect(Collectors.toUnmodifiableMap(Codigado::codigo, Function.identity()));
    }

    public Optional<T> buscar(String codigo) {
        return Optional.ofNullable(porCodigo.get(codigo));
    }
}
