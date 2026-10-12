package com.loja.checkout.web;

import java.math.BigDecimal;

/**
 * Resumo da compra devolvido ao site quando o cálculo dá certo.
 *
 * <p>Todos os valores em dinheiro vêm com 2 casas decimais.
 *
 * @param subtotalProdutos    soma dos produtos
 * @param descontoCupom       desconto do cupom (0 quando não há cupom)
 * @param frete               frete
 * @param prazoEntregaDias    prazo de entrega, em dias
 * @param seguro              seguro do envio
 * @param ajustePagamento     valor final menos o total do pedido (negativo = desconto,
 *                            positivo = tarifa ou juros, zero = não muda nada)
 * @param totalFinal          valor final
 * @param parcelas            em quantas vezes
 * @param valorParcela        valor de cada parcela
 * @param creditoProximaCompra crédito do clube para a próxima compra
 * @param brinde              se vai brinde junto
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
        boolean brinde) {
}
