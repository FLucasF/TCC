package com.loja.checkout.dominio.pagamento;

import java.math.BigDecimal;

/** O que a forma de pagamento faz com o total do pedido. */
public record Cobranca(BigDecimal totalFinal, BigDecimal valorParcela) {
}
