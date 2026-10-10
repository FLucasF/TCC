package com.loja.checkout.dominio;

import java.util.Arrays;
import java.util.Optional;

/**
 * Traducao de codigo recebido do site para a opcao correspondente. Fica num
 * lugar so para que nenhum ponto do calculo precise encadear comparacoes de
 * texto para descobrir de que caso se trata.
 */
public final class Catalogo {

    private Catalogo() {
    }

    public static <E extends Enum<E>> Optional<E> buscar(Class<E> opcoes, String codigo) {
        return Arrays.stream(opcoes.getEnumConstants())
                .filter(opcao -> opcao.name().equals(codigo))
                .findFirst();
    }
}
