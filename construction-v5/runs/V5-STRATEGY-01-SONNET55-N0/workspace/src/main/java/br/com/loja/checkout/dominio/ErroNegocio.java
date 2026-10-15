package br.com.loja.checkout.dominio;

public class ErroNegocio extends RuntimeException {

    private final String codigo;

    public ErroNegocio(String codigo) {
        super(codigo, null, false, false);
        this.codigo = codigo;
    }

    public String codigo() {
        return codigo;
    }
}
