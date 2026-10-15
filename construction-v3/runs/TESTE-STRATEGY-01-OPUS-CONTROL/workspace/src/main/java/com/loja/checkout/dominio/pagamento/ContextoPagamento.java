package com.loja.checkout.dominio.pagamento;

import java.math.BigDecimal;

/**
 * Dados que a forma de pagamento precisa.
 *
 * @param totalPedido          produtos - cupom + frete + imposto
 * @param totalSemImposto      produtos - cupom + frete
 */
public record ContextoPagamento(BigDecimal totalPedido, BigDecimal totalSemImposto) {
}
