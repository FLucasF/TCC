package br.com.loja.checkout.dominio;

/** Pedido recusado: carrega o código do problema que é devolvido ao site. */
public class CheckoutException extends RuntimeException {

    private final CodigoErro codigo;

    public CheckoutException(CodigoErro codigo) {
        super(codigo.name());
        this.codigo = codigo;
    }

    public CodigoErro getCodigo() {
        return codigo;
    }
}
