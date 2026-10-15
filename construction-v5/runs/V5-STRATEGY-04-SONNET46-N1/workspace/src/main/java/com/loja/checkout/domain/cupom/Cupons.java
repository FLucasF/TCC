package com.loja.checkout.domain.cupom;

import java.util.Map;

public class Cupons {
    private static final Map<String, Cupom> REGISTRO = Map.of(
        "BEMVINDO10", new BemVindo10(),
        "MENOS50", new Menos50(),
        "FRETEGRATIS", new FreteGratis(),
        "LEVE3PAGUE2", new Leve3Pague2()
    );

    public static Cupom buscar(String codigo) {
        return REGISTRO.get(codigo);
    }
}
