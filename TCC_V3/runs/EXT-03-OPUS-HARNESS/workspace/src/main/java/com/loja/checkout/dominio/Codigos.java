package com.loja.checkout.dominio;

import java.util.Optional;

/** Busca de um enum pelo codigo recebido na chamada, sem estourar quando nao existe. */
public final class Codigos {

    private Codigos() {
    }

    public static <E extends Enum<E>> Optional<E> buscar(Class<E> tipo, String codigo) {
        if (codigo == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(Enum.valueOf(tipo, codigo));
        } catch (IllegalArgumentException naoExiste) {
            return Optional.empty();
        }
    }
}
