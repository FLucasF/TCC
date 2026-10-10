package br.com.loja.checkout.pedido;

public class CheckoutRecusadoException extends RuntimeException {

    private final ErroCheckout erro;

    public CheckoutRecusadoException(ErroCheckout erro) {
        super(erro.name());
        this.erro = erro;
    }

    public ErroCheckout erro() {
        return erro;
    }
}
