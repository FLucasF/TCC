package com.loja.checkout.dominio;

/** Os problemas que fazem o serviço recusar o pedido. */
public enum Erro {
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

    public PedidoRecusadoException recusar() {
        return new PedidoRecusadoException(this);
    }

    public void recusarSe(boolean condicao) {
        if (condicao) {
            throw recusar();
        }
    }
}
