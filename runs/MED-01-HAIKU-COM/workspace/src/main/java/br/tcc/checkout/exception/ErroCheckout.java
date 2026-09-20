package br.tcc.checkout.exception;

public class ErroCheckout extends RuntimeException {
    private final String codigo;

    public ErroCheckout(String codigo, String mensagem) {
        super(mensagem);
        this.codigo = codigo;
    }

    public String getCodigo() {
        return codigo;
    }
}
