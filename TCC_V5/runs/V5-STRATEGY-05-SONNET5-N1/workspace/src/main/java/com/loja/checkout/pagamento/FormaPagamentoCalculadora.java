package com.loja.checkout.pagamento;

import com.loja.checkout.dominio.FormaPagamento;

import java.math.BigDecimal;

/**
 * Comportamento de uma forma de pagamento. Cada forma tem regra de parcelamento,
 * disponibilidade e fórmula de ajuste próprias, por isso cada uma mora numa
 * implementação isolada, escolhida por lookup num mapa (ver {@link PagamentoRegistry}).
 *
 * A assinatura considera o caso mais exigente (CARTÃO, cujo ajuste depende do número de
 * parcelas e do total do pedido).
 */
public interface FormaPagamentoCalculadora {

    FormaPagamento getTipo();

    boolean parcelasValidas(int parcelas);

    boolean disponivel(BigDecimal totalPedido);

    ResultadoPagamento calcular(BigDecimal totalPedido, int parcelas);
}
