package br.tcc.checkout.exception;

public class ErroCheckout extends RuntimeException {
    private final String codigo;

    public ErroCheckout(String codigo) {
        super(codigo);
        this.codigo = codigo;
    }

    public String getCodigo() {
        return codigo;
    }
}
