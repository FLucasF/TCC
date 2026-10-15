package com.loja.checkout.dominio;

/** Codigos de recusa do pedido, na ordem em que sao conferidos. */
public enum ErroPedido {

    PEDIDO_INVALIDO,
    NIVEL_CLUBE_INVALIDO,
    REGIAO_INVALIDA,
    MODALIDADE_INVALIDA,
    MODALIDADE_INDISPONIVEL,
    CUPOM_INVALIDO,
    CUPOM_NAO_APLICAVEL,
    FORMA_PAGAMENTO_INVALIDA,
    PARCELAMENTO_INVALIDO,
    FORMA_PAGAMENTO_INDISPONIVEL;

    public PedidoRecusadoException erro() {
        return new PedidoRecusadoException(this);
    }

    public void exigir(boolean condicao) {
        if (!condicao) {
            throw erro();
        }
    }
}
