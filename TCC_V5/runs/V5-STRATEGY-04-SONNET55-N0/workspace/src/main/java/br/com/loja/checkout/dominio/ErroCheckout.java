package br.com.loja.checkout.dominio;

public class ErroCheckout extends RuntimeException {

    private final String codigo;

    public ErroCheckout(String codigo) {
        super(codigo, null, false, false);
        this.codigo = codigo;
    }

    public String codigo() {
        return codigo;
    }
}
