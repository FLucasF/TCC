package com.loja.checkout.service;

import com.loja.checkout.enums.CodigoErro;

public class ValidacaoException extends RuntimeException {
    private final CodigoErro codigo;

    public ValidacaoException(CodigoErro codigo) {
        super(codigo.name());
        this.codigo = codigo;
    }

    public CodigoErro getCodigo() {
        return codigo;
    }
}
