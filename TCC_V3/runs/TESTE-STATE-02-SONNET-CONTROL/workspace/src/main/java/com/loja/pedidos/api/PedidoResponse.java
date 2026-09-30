package com.loja.pedidos.api;

import com.loja.pedidos.dominio.Pedido;
import com.loja.pedidos.dominio.Situacao;

import java.math.BigDecimal;
import java.util.List;

public record PedidoResponse(
        String id,
        Situacao situacao,
        String descricao,
        BigDecimal valorProdutos,
        BigDecimal frete,
        BigDecimal valorTotal,
        BigDecimal valorReembolsado,
        boolean estoqueDevolvido,
        boolean coletaAgendada,
        List<Situacao> historico
) {

    public static PedidoResponse de(Pedido pedido) {
        return new PedidoResponse(
                pedido.getId(),
                pedido.getSituacao(),
                pedido.getSituacao().getDescricao(),
                pedido.getValorProdutos().setScale(2),
                pedido.getFrete().setScale(2),
                pedido.getValorTotal().setScale(2),
                pedido.getValorReembolsado().setScale(2),
                pedido.isEstoqueDevolvido(),
                pedido.isColetaAgendada(),
                pedido.getHistorico()
        );
    }
}
