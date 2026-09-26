package br.com.loja.checkout.pagamento;

import java.math.BigDecimal;

/** Como o pedido fica depois do ajuste da forma de pagamento. */
public record ResultadoPagamento(BigDecimal totalFinal, BigDecimal valorParcela) {
}
