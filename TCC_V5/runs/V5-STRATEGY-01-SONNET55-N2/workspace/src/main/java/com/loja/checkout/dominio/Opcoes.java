package com.loja.checkout.dominio;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class Opcoes<T extends Opcao> {

    private final Map<String, T> porCodigo;

    public Opcoes(List<T> opcoes) {
        this.porCodigo = opcoes.stream().collect(Collectors.toMap(Opcao::codigo, Function.identity()));
    }

    public Optional<T> buscar(String codigo) {
        return Optional.ofNullable(codigo).map(porCodigo::get);
    }
}
