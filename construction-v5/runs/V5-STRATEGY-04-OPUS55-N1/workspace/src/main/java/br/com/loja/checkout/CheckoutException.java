package br.com.loja.checkout;

/** Pedido recusado; o código identifica o problema encontrado. */
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
