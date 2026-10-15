package com.loja.resumo;

class ErroCompra extends RuntimeException {

    private final CodigoErro codigo;

    ErroCompra(CodigoErro codigo) {
        super(codigo.name());
        this.codigo = codigo;
    }

    CodigoErro codigo() {
        return codigo;
    }
}
