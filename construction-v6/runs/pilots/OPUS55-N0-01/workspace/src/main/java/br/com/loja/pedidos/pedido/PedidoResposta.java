package br.com.loja.pedidos.pedido;

import java.util.List;

import br.com.loja.pedidos.checkout.Resumo;

/** O pedido como o site enxerga. */
public record PedidoResposta(long id, SituacaoPedido situacao, String criadoEm, Resumo resumo, List<ItemPedido> itens,
        String codigoRastreio) {

    public static PedidoResposta de(Pedido pedido) {
        return new PedidoResposta(pedido.id(), pedido.situacao(), pedido.criadoEm().toString(), pedido.resumo(),
                pedido.itens(), pedido.codigoRastreio());
    }
}
