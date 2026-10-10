package br.com.loja.checkout.dominio;

/** Pedido que nao da para calcular. Carrega o codigo do problema. */
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
