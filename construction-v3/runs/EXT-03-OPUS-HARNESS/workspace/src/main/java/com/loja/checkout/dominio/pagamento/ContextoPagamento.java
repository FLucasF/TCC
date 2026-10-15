package com.loja.checkout.dominio.pagamento;

import java.math.BigDecimal;

/**
 * O que a forma de pagamento pode olhar para decidir se atende o pedido.
 * O teto do boleto e sobre produtos - cupom + frete, sem o imposto.
 */
public record ContextoPagamento(BigDecimal totalSemImposto) {
}
