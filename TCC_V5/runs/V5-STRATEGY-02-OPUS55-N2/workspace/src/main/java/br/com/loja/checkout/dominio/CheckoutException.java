package br.com.loja.checkout.dominio;

public class CheckoutException extends RuntimeException {

    private final Erro erro;

    public CheckoutException(Erro erro) {
        super(erro.name());
        this.erro = erro;
    }

    public Erro erro() {
        return erro;
    }
}
