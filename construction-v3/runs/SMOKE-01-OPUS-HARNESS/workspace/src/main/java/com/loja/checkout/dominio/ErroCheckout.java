package com.loja.checkout.dominio;

/** Recusa de calculo do resumo, identificada pelo codigo devolvido ao site. */
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
