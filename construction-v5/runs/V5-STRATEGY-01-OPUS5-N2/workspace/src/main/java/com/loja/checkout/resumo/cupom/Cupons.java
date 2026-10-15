package com.loja.checkout.resumo.cupom;

import java.util.Map;
import java.util.Optional;

/** As promoções que valem hoje, pelo código que o cliente digita. */
public final class Cupons {

    private static final Map<String, Cupom> POR_CODIGO = Map.of(
            "BEMVINDO10", new Bemvindo10(),
            "MENOS50", new Menos50(),
            "FRETEGRATIS", new FreteGratis(),
            "LEVE3PAGUE2", new Leve3Pague2());

    private Cupons() {
    }

    public static Optional<Cupom> porCodigo(String codigo) {
        return Optional.ofNullable(codigo).map(POR_CODIGO::get);
    }
}
