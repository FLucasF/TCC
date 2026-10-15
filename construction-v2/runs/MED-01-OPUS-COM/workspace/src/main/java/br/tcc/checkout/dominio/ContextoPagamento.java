package br.tcc.checkout.dominio;

import java.math.BigDecimal;

/** Tudo que uma forma de pagamento pode olhar para cobrar o pedido. */
public record ContextoPagamento(BigDecimal totalPedido, int parcelas) {
}
