package com.loja.checkout.web;

import java.math.BigDecimal;

/**
 * Resumo da compra devolvido quando o pedido pode ser calculado.
 *
 * <p>Todos os valores em dinheiro vem com 2 casas decimais.</p>
 */
public record CheckoutResponse(
        BigDecimal subtotalProdutos,
        BigDecimal descontoCupom,
        BigDecimal frete,
        int prazoEntregaDias,
        BigDecimal seguro,
        BigDecimal ajustePagamento,
        BigDecimal totalFinal,
        int parcelas,
        BigDecimal valorParcela,
        BigDecimal creditoProximaCompra,
        boolean brinde
) {
}
