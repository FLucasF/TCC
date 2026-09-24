package com.loja.checkout.dominio;

import java.math.BigDecimal;

/** Como a forma de pagamento fecha o pedido: valor final e parcela. */
public record ResultadoPagamento(BigDecimal totalFinal, int parcelas, BigDecimal valorParcela) {
}
