package br.com.loja.checkout;

/** Pedido recusado: carrega o código do problema devolvido ao site. */
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
