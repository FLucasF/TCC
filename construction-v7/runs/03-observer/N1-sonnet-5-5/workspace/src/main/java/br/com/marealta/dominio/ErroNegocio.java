package br.com.marealta.dominio;

public class ErroNegocio extends RuntimeException {

    private final CodigoErro codigo;

    public ErroNegocio(CodigoErro codigo, String mensagem) {
        super(mensagem);
        this.codigo = codigo;
    }

    public CodigoErro codigo() {
        return codigo;
    }
}
