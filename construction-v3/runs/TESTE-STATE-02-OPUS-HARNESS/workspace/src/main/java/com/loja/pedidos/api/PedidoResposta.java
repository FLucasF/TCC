package com.loja.pedidos.api;

import com.loja.pedidos.dominio.Pedido;
import com.loja.pedidos.dominio.Situacao;

import java.math.BigDecimal;
import java.util.List;

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
                pedido.id(),
                pedido.situacao(),
                pedido.situacao().descricao(),
                pedido.valorProdutos(),
                pedido.frete(),
                pedido.valorTotal(),
                pedido.valorReembolsado(),
                pedido.estoqueDevolvido(),
                pedido.coletaAgendada(),
                pedido.historico());
    }
}
