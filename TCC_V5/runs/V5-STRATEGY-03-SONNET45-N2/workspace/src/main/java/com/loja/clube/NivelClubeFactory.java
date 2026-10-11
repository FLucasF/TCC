package com.loja.clube;

import com.loja.exception.PedidoException;
import java.util.Map;

public class NivelClubeFactory {
    private static final Map<String, NivelClube> NIVEIS = Map.of(
        "BRONZE", new Bronze(),
        "PRATA", new Prata(),
        "OURO", new Ouro()
    );

    public static NivelClube criar(String codigo) {
        if (codigo == null || codigo.isBlank()) {
            throw new PedidoException("NIVEL_CLUBE_INVALIDO");
        }

        NivelClube nivel = NIVEIS.get(codigo);
        if (nivel == null) {
            throw new PedidoException("NIVEL_CLUBE_INVALIDO");
        }

        return nivel;
    }
}
