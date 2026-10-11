package com.loja.checkout.erro;

/** Erro de negócio que vira a resposta { "erro": codigo }. */
public class PedidoException extends RuntimeException {

    private final String codigo;

    public PedidoException(String codigo) {
        super(codigo);
        this.codigo = codigo;
    }

    public String getCodigo() {
        return codigo;
    }
}
