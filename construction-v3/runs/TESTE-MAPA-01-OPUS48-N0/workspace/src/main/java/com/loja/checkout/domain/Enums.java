package com.loja.checkout.domain;

import java.util.Optional;

/** Conversão tolerante de texto para enum: desconhecido/nulo vira vazio. */
final class Enums {

    private Enums() {
    }

    static <E extends Enum<E>> Optional<E> fromNome(Class<E> tipo, String nome) {
        if (nome == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(Enum.valueOf(tipo, nome));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
