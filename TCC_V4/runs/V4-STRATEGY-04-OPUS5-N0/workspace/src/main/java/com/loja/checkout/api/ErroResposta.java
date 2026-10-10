package com.loja.checkout.api;

import com.loja.checkout.dominio.CodigoErro;

/** Resposta de pedido recusado: so o codigo do problema. */
public record ErroResposta(String erro) {

    public static ErroResposta de(CodigoErro codigo) {
        return new ErroResposta(codigo.name());
    }
}
