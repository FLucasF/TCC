package com.loja.checkout.resumo.clube;

import java.util.Map;
import java.util.Optional;

/** Os níveis do clube, pelo código que o site envia. */
public final class NiveisClube {

    private static final Map<String, NivelClube> POR_CODIGO = Map.of(
            "BRONZE", new Bronze(),
            "PRATA", new Prata(),
            "OURO", new Ouro());

    private NiveisClube() {
    }

    public static Optional<NivelClube> porCodigo(String codigo) {
        return Optional.ofNullable(codigo).map(POR_CODIGO::get);
    }
}
