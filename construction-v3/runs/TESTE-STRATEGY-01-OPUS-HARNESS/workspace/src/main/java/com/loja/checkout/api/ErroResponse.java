package com.loja.checkout.api;

import com.loja.checkout.dominio.ErroCheckout;

public record ErroResponse(String erro) {

    public static ErroResponse de(ErroCheckout erro) {
        return new ErroResponse(erro.name());
    }
}
