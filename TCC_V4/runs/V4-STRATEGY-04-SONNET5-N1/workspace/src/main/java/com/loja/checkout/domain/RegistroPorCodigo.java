package com.loja.checkout.domain;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

public class RegistroPorCodigo<T extends Codificavel> {

    private final Map<String, T> porCodigo;

    public RegistroPorCodigo(List<T> itens) {
        this.porCodigo = itens.stream().collect(Collectors.toMap(Codificavel::getCodigo, Function.identity()));
    }

    public Optional<T> buscar(String codigo) {
        if (codigo == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(porCodigo.get(codigo));
    }
}
