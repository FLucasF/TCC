package com.loja.pedidos.api.dto;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import com.loja.pedidos.dominio.Pedido;

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
                pedido.id(),
                pedido.situacao().name(),
                pedido.situacao().descricao(),
                comDuasCasas(pedido.valorProdutos()),
                comDuasCasas(pedido.frete()),
                comDuasCasas(pedido.valorTotal()),
                comDuasCasas(pedido.valorReembolsado()),
                pedido.estoqueDevolvido(),
                pedido.coletaAgendada(),
                pedido.historico().stream().map(Enum::name).toList());
    }

    private static BigDecimal comDuasCasas(BigDecimal valor) {
        return valor.setScale(2, RoundingMode.HALF_UP);
    }
}
