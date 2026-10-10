package com.loja.checkout.dominio;

import com.loja.checkout.erro.Codigo;
import com.loja.checkout.erro.PedidoRecusado;

import java.util.Arrays;
import java.util.Optional;

/**
 * Acha a opcao escolhida pelo cliente pelo codigo que o site mandou. O codigo
 * precisa bater exatamente, em letras maiusculas.
 */
public final class Opcao {

    private Opcao() {
    }

    public static <E extends Enum<E>> E exigir(Class<E> tipo, String codigo, Codigo seInvalida) {
        return procurar(tipo, codigo).orElseThrow(() -> new PedidoRecusado(seInvalida));
    }

    public static <E extends Enum<E>> Optional<E> procurar(Class<E> tipo, String codigo) {
        return Optional.ofNullable(codigo)
                .flatMap(informado -> Arrays.stream(tipo.getEnumConstants())
                        .filter(opcao -> opcao.name().equals(informado))
                        .findFirst());
    }
}
