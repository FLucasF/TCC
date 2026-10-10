package com.loja.checkout.factory;

import com.loja.checkout.domain.Regiao;
import com.loja.checkout.domain.regiao.*;
import java.util.Map;

public class RegiaoFactory {

    private static final Map<String, Regiao> REGIOES = Map.of(
        "SUDESTE", new Sudeste(),
        "SUL", new Sul(),
        "CENTRO_OESTE", new CentroOeste(),
        "NORTE", new Norte(),
        "NORDESTE", new Nordeste()
    );

    public static Regiao obter(String codigo) {
        return REGIOES.get(codigo);
    }

    public static boolean existe(String codigo) {
        return REGIOES.containsKey(codigo);
    }
}
