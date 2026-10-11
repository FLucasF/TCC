package com.loja.checkout.pagamento;

import java.math.BigDecimal;

/**
 * Uma forma de pagamento. Cada forma tem sua própria regra de parcelamento,
 * disponibilidade e ajuste sobre o total do pedido (desconto, tarifa ou
 * juros).
 */
public interface FormaPagamento {

    String codigo();

    boolean parcelasValidas(int parcelas);

    boolean disponivel(BigDecimal totalPedido);

    ResultadoPagamento calcular(BigDecimal totalPedido, int parcelas);
}
