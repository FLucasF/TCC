package com.loja.pedidos.web;

/** Erro de negócio que vira a resposta {"erro": "CODIGO"}. */
public class ErroDePedidoException extends RuntimeException {

    private final CodigoDeErro codigo;

    public ErroDePedidoException(CodigoDeErro codigo) {
        super(codigo.name());
        this.codigo = codigo;
    }

    public CodigoDeErro getCodigo() {
        return codigo;
    }
}
