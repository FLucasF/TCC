package com.loja.checkout.dominio.cupom;

import com.loja.checkout.dominio.Catalogo;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

/** As promoções que valem hoje. */
public final class Cupons {

    public static final Catalogo<Cupom> CATALOGO = new Catalogo<>(
            List.of(new Bemvindo10(), new Menos50(), new FreteGratis(), new Leve3Pague2()).stream()
                    .collect(Collectors.toMap(Cupom::codigo, Function.identity())),
            "CUPOM_INVALIDO");

    private Cupons() {
    }
}
