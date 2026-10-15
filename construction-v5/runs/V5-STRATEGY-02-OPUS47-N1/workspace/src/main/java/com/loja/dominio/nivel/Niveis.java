package com.loja.dominio.nivel;

import com.loja.dominio.ErroCheckout;

import java.util.Map;
import java.util.function.Supplier;

public final class Niveis {
    private static final Map<String, Supplier<NivelClube>> REGISTRO = Map.of(
        "BRONZE", Bronze::new,
        "PRATA", Prata::new,
        "OURO", Ouro::new
    );

    private Niveis() {}

    public static NivelClube parse(String codigo) {
        if (codigo == null) throw new ErroCheckout("NIVEL_CLUBE_INVALIDO");
        var sup = REGISTRO.get(codigo);
        if (sup == null) throw new ErroCheckout("NIVEL_CLUBE_INVALIDO");
        return sup.get();
    }
}
