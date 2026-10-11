package com.loja.checkout.dominio.clube;

import com.loja.checkout.dominio.Catalogo;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Os níveis do clube que existem hoje. */
public final class Niveis {

    public static final Catalogo<NivelClube> CATALOGO = new Catalogo<>(
            List.of(new Bronze(), new Prata(), new Ouro()).stream()
                    .collect(Collectors.toMap(NivelClube::codigo, Function.identity())),
            "NIVEL_CLUBE_INVALIDO");

    private Niveis() {
    }
}
