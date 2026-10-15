package com.loja.checkout.api;

import com.loja.checkout.dominio.CodigoErro;

public record ErroResponse(String erro) {

    static ErroResponse de(CodigoErro codigo) {
        return new ErroResponse(codigo.name());
    }
}
