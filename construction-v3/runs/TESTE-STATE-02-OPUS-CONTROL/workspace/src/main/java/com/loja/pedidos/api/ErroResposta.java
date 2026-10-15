package com.loja.pedidos.api;

import com.loja.pedidos.dominio.ErroPedido;

/** Corpo de qualquer resposta de erro. */
public record ErroResposta(String erro) {

    public static ErroResposta de(ErroPedido erro) {
        return new ErroResposta(erro.name());
    }
}
