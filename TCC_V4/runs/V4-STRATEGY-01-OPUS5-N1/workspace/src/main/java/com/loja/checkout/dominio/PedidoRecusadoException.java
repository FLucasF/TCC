package com.loja.checkout.dominio;

/** Interrompe o calculo quando o pedido nao pode ser atendido. */
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
