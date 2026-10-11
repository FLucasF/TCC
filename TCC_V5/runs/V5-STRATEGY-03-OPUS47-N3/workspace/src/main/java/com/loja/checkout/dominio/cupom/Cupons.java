package com.loja.checkout.dominio.cupom;

import java.util.List;
import java.util.Optional;

public final class Cupons {
    private static final List<Cupom> TODOS = List.of(
            new Bemvindo10(),
            new Menos50(),
            new FreteGratis(),
            new Leve3Pague2()
    );

    private Cupons() {}

    public static Optional<Cupom> buscar(String codigo) {
        if (codigo == null) return Optional.empty();
        return TODOS.stream().filter(c -> c.codigo().equals(codigo)).findFirst();
    }
}
