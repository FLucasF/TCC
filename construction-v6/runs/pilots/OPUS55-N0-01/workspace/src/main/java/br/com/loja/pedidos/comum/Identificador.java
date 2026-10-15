package br.com.loja.pedidos.comum;

import java.util.Optional;

public final class Identificador {

    private Identificador() {
    }

    /** Lê o identificador numérico que vem no endereço; vazio quando não é um número válido. */
    public static Optional<Long> ler(String texto) {
        try {
            long id = Long.parseLong(texto);
            return id > 0 ? Optional.of(id) : Optional.empty();
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }
}
