package loja.checkout;

public class PedidoRecusadoException extends RuntimeException {

    private final ErroCheckout erro;

    public PedidoRecusadoException(ErroCheckout erro) {
        super(erro.name(), null, false, false);
        this.erro = erro;
    }

    public ErroCheckout erro() {
        return erro;
    }
}
