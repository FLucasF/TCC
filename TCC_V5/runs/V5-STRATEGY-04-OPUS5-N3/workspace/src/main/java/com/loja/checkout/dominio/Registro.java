package com.loja.checkout.dominio;

import java.util.Collection;
import java.util.Optional;
import java.util.function.Function;

/** Resolve código -> caso, sem cadeia de condições. */
public final class Registro {

    private Registro() {
    }

    public static <T> Optional<T> busca(Collection<T> casos, Function<T, String> codigo, String procurado) {
        if (procurado == null) {
            return Optional.empty();
        }
        return casos.stream()
                .filter(caso -> codigo.apply(caso).equals(procurado))
                .findFirst();
    }
}
