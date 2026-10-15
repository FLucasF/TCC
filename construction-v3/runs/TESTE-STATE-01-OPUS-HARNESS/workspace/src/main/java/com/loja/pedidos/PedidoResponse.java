package com.loja.pedidos;

import java.math.BigDecimal;
import java.util.List;

public record PedidoResponse(
        String id,
        String situacao,
        String descricao,
        BigDecimal valorProdutos,
        BigDecimal frete,
        BigDecimal valorTotal,
        BigDecimal valorReembolsado,
        boolean estoqueDevolvido,
        boolean coletaAgendada,
        List<String> historico) {

    public static PedidoResponse de(Pedido pedido) {
        return new PedidoResponse(
                pedido.getId(),
                pedido.getSituacao().name(),
                pedido.getSituacao().getDescricao(),
                pedido.getValorProdutos(),
                pedido.getFrete(),
                pedido.getValorTotal(),
                pedido.getValorReembolsado(),
                pedido.isEstoqueDevolvido(),
                pedido.isColetaAgendada(),
                pedido.getHistorico().stream().map(Situacao::name).toList());
    }
}
