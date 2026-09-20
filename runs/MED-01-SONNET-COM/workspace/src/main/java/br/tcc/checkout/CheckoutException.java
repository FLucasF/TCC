package br.tcc.checkout;

class CheckoutException extends RuntimeException {

    private final ErroCodigo codigo;

    CheckoutException(ErroCodigo codigo) {
        super(codigo.name());
        this.codigo = codigo;
    }

    ErroCodigo getCodigo() {
        return codigo;
    }
}
