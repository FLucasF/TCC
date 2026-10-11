package br.com.loja.checkout.dominio.pagamento;

import java.math.BigDecimal;

/** Como o total do pedido vai ser cobrado do cliente. */
public record Cobranca(BigDecimal totalFinal, int parcelas, BigDecimal valorParcela) {
}
