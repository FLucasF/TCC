package com.loja.checkout.dominio;

/**
 * Pedido que não pode ser calculado. Carrega o código do problema, conforme a
 * ordem de verificação do enunciado.
 */
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
