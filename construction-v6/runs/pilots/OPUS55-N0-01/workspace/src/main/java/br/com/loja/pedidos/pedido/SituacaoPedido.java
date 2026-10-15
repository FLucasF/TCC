package br.com.loja.pedidos.pedido;

import java.util.Set;

/** As situações do pedido e para onde cada uma pode ir. */
public enum SituacaoPedido {

    AGUARDANDO_PAGAMENTO(false),
    PAGO(true),
    ENVIADO(true),
    ENTREGUE(true),
    CANCELADO(false);

    static {
        AGUARDANDO_PAGAMENTO.seguintes = Set.of(PAGO, CANCELADO);
        PAGO.seguintes = Set.of(ENVIADO, CANCELADO);
        ENVIADO.seguintes = Set.of(ENTREGUE);
        ENTREGUE.seguintes = Set.of();
        CANCELADO.seguintes = Set.of();
    }

    private final boolean pago;
    private Set<SituacaoPedido> seguintes;

    SituacaoPedido(boolean pago) {
        this.pago = pago;
    }

    /** Se o pagamento já foi confirmado: estoque baixado, crédito concedido e o pedido conta como compra. */
    public boolean pago() {
        return pago;
    }

    public boolean podeIrPara(SituacaoPedido destino) {
        return seguintes.contains(destino);
    }
}
