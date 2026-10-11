package com.loja.checkout.dominio.clube;

import java.util.List;
import java.util.Optional;

public final class Niveis {
    private static final List<Nivel> TODOS = List.of(new Bronze(), new Prata(), new Ouro());

    private Niveis() {}

    public static Optional<Nivel> buscar(String codigo) {
        if (codigo == null) return Optional.empty();
        return TODOS.stream().filter(n -> n.codigo().equals(codigo)).findFirst();
    }
}
