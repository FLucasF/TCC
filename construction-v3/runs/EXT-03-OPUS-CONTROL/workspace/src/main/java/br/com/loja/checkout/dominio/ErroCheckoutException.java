package br.com.loja.checkout.dominio;

/** Erro de negocio do checkout: vira uma resposta 400 com o codigo correspondente. */
public class ErroCheckoutException extends RuntimeException {

    private final CodigoErro codigo;

    public ErroCheckoutException(CodigoErro codigo) {
        super(codigo.name());
        this.codigo = codigo;
    }

    public CodigoErro codigo() {
        return codigo;
    }
}
