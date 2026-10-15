package com.loja.checkout.pagamento;

import java.math.BigDecimal;

/**
 * Dados do pedido sobre os quais a forma de pagamento age.
 *
 * @param totalPedido     produtos - cupom + frete + imposto: base do ajuste
 * @param totalSemImposto produtos - cupom + frete: base dos limites comerciais
 * @param parcelas        numero de parcelas escolhido
 */
public record ContextoPagamento(BigDecimal totalPedido, BigDecimal totalSemImposto, int parcelas) {
}
