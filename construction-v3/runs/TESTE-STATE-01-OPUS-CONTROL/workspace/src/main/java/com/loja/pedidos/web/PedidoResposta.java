package com.loja.pedidos.web;

import com.loja.pedidos.dominio.Pedido;
import com.loja.pedidos.dominio.Situacao;
import java.math.BigDecimal;
import java.util.List;

/** O pedido como o site espera receber. */
public record PedidoResposta(
        String id,
        Situacao situacao,
        String descricao,
        BigDecimal valorProdutos,
        BigDecimal frete,
        BigDecimal valorTotal,
        BigDecimal valorReembolsado,
        boolean estoqueDevolvido,
        boolean coletaAgendada,
        List<Situacao> historico) {

    public static PedidoResposta de(Pedido pedido) {
        return new PedidoResposta(
                pedido.getId(),
                pedido.getSituacao(),
                pedido.getSituacao().getDescricao(),
                pedido.getValorProdutos(),
                pedido.getFrete(),
                pedido.getValorTotal(),
                pedido.getValorReembolsado(),
                pedido.isEstoqueDevolvido(),
                pedido.isColetaAgendada(),
                pedido.getHistorico());
    }
}
