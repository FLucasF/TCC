package com.loja.checkout.dominio;

/** Pedido que não pode ser calculado: carrega o código do problema. */
public class PedidoRecusadoException extends RuntimeException {

    private final CodigoErro codigo;

    public PedidoRecusadoException(CodigoErro codigo) {
        super(codigo.name());
        this.codigo = codigo;
    }

    public CodigoErro getCodigo() {
        return codigo;
    }
}
