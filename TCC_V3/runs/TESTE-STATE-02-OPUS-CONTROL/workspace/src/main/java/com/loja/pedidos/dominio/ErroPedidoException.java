package com.loja.pedidos.dominio;

/** Interrompe o atendimento da requisicao com um dos erros conhecidos. */
public class ErroPedidoException extends RuntimeException {

    private final ErroPedido erro;

    public ErroPedidoException(ErroPedido erro) {
        super(erro.name());
        this.erro = erro;
    }

    public ErroPedido erro() {
        return erro;
    }
}
