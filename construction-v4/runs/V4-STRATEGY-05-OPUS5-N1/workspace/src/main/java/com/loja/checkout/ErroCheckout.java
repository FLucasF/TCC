package com.loja.checkout;

/** Pedido recusado: carrega so o codigo do problema, como o site espera. */
public class ErroCheckout extends RuntimeException {

    private final String codigo;

    public ErroCheckout(String codigo) {
        super(codigo);
        this.codigo = codigo;
    }

    public String codigo() {
        return codigo;
    }
}
