package com.loja.checkout.dominio;

/** Recusa do pedido: carrega só o código do problema, que é o que o site recebe. */
public class ErroPedido extends RuntimeException {

    private final String codigo;

    public ErroPedido(String codigo) {
        super(codigo);
        this.codigo = codigo;
    }

    public String codigo() {
        return codigo;
    }
}
