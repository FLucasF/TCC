package br.com.loja.checkout.dominio;

public class ErroCheckout extends RuntimeException {

    private final CodigoErro codigo;

    public ErroCheckout(CodigoErro codigo) {
        super(codigo.name());
        this.codigo = codigo;
    }

    public CodigoErro codigo() {
        return codigo;
    }
}
