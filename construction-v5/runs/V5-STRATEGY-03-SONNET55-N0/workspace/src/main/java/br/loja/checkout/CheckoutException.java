package br.loja.checkout;

/** Pedido recusado; carrega apenas o código do problema. */
public class CheckoutException extends RuntimeException {
    private final String codigo;

    public CheckoutException(String codigo) {
        super(codigo);
        this.codigo = codigo;
    }

    public String codigo() {
        return codigo;
    }
}
