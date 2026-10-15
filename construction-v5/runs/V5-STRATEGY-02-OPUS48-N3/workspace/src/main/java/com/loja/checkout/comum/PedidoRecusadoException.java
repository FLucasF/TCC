package com.loja.checkout.comum;

/**
 * Pedido que não dá para calcular. Carrega só o código do problema, que é tudo
 * o que o serviço devolve nesse caso.
 */
public class PedidoRecusadoException extends RuntimeException {

    private final String codigo;

    public PedidoRecusadoException(String codigo) {
        super(codigo);
        this.codigo = codigo;
    }

    public String codigo() {
        return codigo;
    }
}
