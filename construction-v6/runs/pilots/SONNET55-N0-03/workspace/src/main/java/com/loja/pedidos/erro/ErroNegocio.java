package com.loja.pedidos.erro;

public class ErroNegocio extends RuntimeException {

    private final CodigoErro codigo;

    public ErroNegocio(CodigoErro codigo) {
        super(codigo.name(), null, false, false);
        this.codigo = codigo;
    }

    public CodigoErro codigo() {
        return codigo;
    }
}
