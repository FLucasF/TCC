package com.loja.checkout.dominio;

/** O pedido nao pode ser calculado: carrega o codigo do problema encontrado. */
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
