package com.loja.checkout.api;

import com.loja.checkout.dominio.CodigoErro;

/** Pedido que nao da para calcular. */
public class PedidoRecusadoException extends RuntimeException {

    private final CodigoErro codigo;

    public PedidoRecusadoException(CodigoErro codigo) {
        super(codigo.name());
        this.codigo = codigo;
    }

    public CodigoErro codigo() {
        return codigo;
    }
}
