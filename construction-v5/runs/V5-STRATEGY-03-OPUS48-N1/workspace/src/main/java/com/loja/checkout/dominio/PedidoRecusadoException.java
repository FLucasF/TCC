package com.loja.checkout.dominio;

/**
 * Lancada quando o pedido nao pode ser calculado. Carrega so o codigo do
 * problema, que e o que o servico devolve ao site.
 */
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
