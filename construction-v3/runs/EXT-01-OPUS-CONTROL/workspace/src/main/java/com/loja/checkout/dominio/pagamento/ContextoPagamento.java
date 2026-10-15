package com.loja.checkout.dominio.pagamento;

import java.math.BigDecimal;

/**
 * Dados do pedido que a forma de pagamento usa.
 *
 * @param totalPedido      produtos - cupom + frete + imposto
 * @param totalSemImposto  produtos - cupom + frete
 * @param parcelas         numero de parcelas escolhido
 */
public record ContextoPagamento(BigDecimal totalPedido, BigDecimal totalSemImposto, int parcelas) {
}
