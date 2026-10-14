package br.com.loja.pedidos.pedido;

/**
 * As situações do pedido e as mudanças permitidas entre elas. Para criar uma situação nova
 * (por exemplo, "em separação" entre o pago e o enviado), basta incluí-la aqui e ajustar
 * {@link #podeIrPara}.
 */
public enum SituacaoPedido {
    AGUARDANDO_PAGAMENTO(false),
    PAGO(true),
    ENVIADO(true),
    ENTREGUE(true),
    CANCELADO(false);

    private final boolean pagamentoConfirmado;

    SituacaoPedido(boolean pagamentoConfirmado) {
        this.pagamentoConfirmado = pagamentoConfirmado;
    }

    /**
     * Se o pagamento deste pedido está valendo: o estoque já foi baixado e o crédito do clube já
     * foi dado. Também é o que conta como compra feita (para o cupom de primeira compra).
     */
    public boolean pagamentoConfirmado() {
        return pagamentoConfirmado;
    }

    public boolean podeIrPara(SituacaoPedido destino) {
        return switch (this) {
            case AGUARDANDO_PAGAMENTO -> destino == PAGO || destino == CANCELADO;
            case PAGO -> destino == ENVIADO || destino == CANCELADO;
            case ENVIADO -> destino == ENTREGUE;
            case ENTREGUE, CANCELADO -> false;
        };
    }
}
