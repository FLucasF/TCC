package com.loja.checkout.dominio;

/** Recusa do pedido; o código vai para o site no campo "erro". */
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
