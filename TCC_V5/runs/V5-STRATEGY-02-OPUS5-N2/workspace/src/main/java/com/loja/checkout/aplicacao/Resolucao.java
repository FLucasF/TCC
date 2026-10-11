package com.loja.checkout.aplicacao;

import com.loja.checkout.dominio.CodigoErro;
import com.loja.checkout.dominio.PedidoRecusado;

import java.util.Optional;

/** Traduz o que o site mandou em opção conhecida, ou recusa o pedido. */
final class Resolucao {

    private Resolucao() {
    }

    static <E extends Enum<E>> E obrigatoria(Class<E> opcoes, String informado, CodigoErro erro) {
        return opcional(opcoes, informado, erro).orElseThrow(() -> new PedidoRecusado(erro));
    }

    static <E extends Enum<E>> Optional<E> opcional(Class<E> opcoes, String informado, CodigoErro erro) {
        if (informado == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(Enum.valueOf(opcoes, informado));
        } catch (IllegalArgumentException naoExiste) {
            throw new PedidoRecusado(erro);
        }
    }
}
