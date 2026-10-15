package com.loja.checkout.dominio.cupom;

import java.util.Map;
import java.util.Optional;

public final class Cupons {
    private static final Map<String, Cupom> MAP = Map.of(
            "BEMVINDO10", new Bemvindo10(),
            "MENOS50", new Menos50(),
            "FRETEGRATIS", new FreteGratis(),
            "LEVE3PAGUE2", new Leve3Pague2()
    );

    private Cupons() {}

    public static Optional<Cupom> buscar(String codigo) {
        return Optional.ofNullable(codigo).map(MAP::get);
    }
}
