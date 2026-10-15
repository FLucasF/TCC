package com.loja.checkout.dominio.pagamento;

import java.math.BigDecimal;

/** Como o pagamento escolhido mexe no total do pedido. */
public record ResultadoPagamento(BigDecimal totalFinal, int parcelas, BigDecimal valorParcela) {
}
