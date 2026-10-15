package com.loja.checkout.dominio;

import java.util.Arrays;
import java.util.Optional;

/**
 * Resolve o enum a partir do codigo que o site mandou (texto exato, em
 * maiusculas). Um lugar so para "esse codigo existe?", usado por todas as
 * familias de caso (entrega, cupom, pagamento, clube, regiao).
 */
public final class PorCodigo {

    private PorCodigo() {
    }

    public static <E extends Enum<E>> Optional<E> resolver(Class<E> tipo, String codigo) {
        if (codigo == null) {
            return Optional.empty();
        }
        return Arrays.stream(tipo.getEnumConstants())
                .filter(valor -> valor.name().equals(codigo))
                .findFirst();
    }
}
