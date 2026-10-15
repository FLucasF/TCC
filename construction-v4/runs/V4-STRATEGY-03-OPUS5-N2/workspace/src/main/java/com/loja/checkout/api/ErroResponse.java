package com.loja.checkout.api;

/** Resposta de pedido recusado: so o codigo do problema. */
public record ErroResponse(String erro) {

    public ErroResponse(CodigoErro codigo) {
        this(codigo.name());
    }
}
