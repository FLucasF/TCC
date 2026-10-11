package com.loja.checkout.dominio;

/**
 * Lançada quando o pedido não pode ser calculado. Carrega só o código do
 * problema, que é o que o serviço devolve ao site.
 */
public class PedidoRejeitadoException extends RuntimeException {

    private final String codigo;

    public PedidoRejeitadoException(String codigo) {
        super(codigo);
        this.codigo = codigo;
    }

    public String codigo() {
        return codigo;
    }
}
