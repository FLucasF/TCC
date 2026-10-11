package com.loja.checkout.model;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public enum Cupom {
    BEMVINDO10("BEMVINDO10"),
    MENOS50("MENOS50"),
    FRETEGRATIS("FRETEGRATIS"),
    LEVE3PAGUE2("LEVE3PAGUE2");

    private final String codigo;
    private static final Map<String, Cupom> BY_CODIGO = Arrays.stream(Cupom.values())
            .collect(Collectors.toUnmodifiableMap(c -> c.codigo, c -> c));

    Cupom(String codigo) {
        this.codigo = codigo;
    }

    public String getCodigo() {
        return codigo;
    }

    public static Optional<Cupom> byCodigo(String codigo) {
        return Optional.ofNullable(BY_CODIGO.get(codigo));
    }
}
