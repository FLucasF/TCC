package com.loja.regiao;

import com.loja.exception.PedidoException;
import java.util.Map;

public class RegiaoFactory {
    private static final Map<String, Regiao> REGIOES = Map.of(
        "SUDESTE", new Sudeste(),
        "SUL", new Sul(),
        "CENTRO_OESTE", new CentroOeste(),
        "NORTE", new Norte(),
        "NORDESTE", new Nordeste()
    );

    public static Regiao criar(String codigo) {
        if (codigo == null || codigo.isBlank()) {
            throw new PedidoException("REGIAO_INVALIDA");
        }

        Regiao regiao = REGIOES.get(codigo);
        if (regiao == null) {
            throw new PedidoException("REGIAO_INVALIDA");
        }

        return regiao;
    }
}
