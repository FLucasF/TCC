package com.loja.checkout.dominio.pagamento;

import java.math.BigDecimal;

/** Como o pedido fica depois do ajuste da forma de pagamento. */
public record Cobranca(BigDecimal valorFinal, int parcelas, BigDecimal valorParcela) {
}
