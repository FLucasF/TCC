package com.loja.checkout.entrega;

import com.loja.checkout.dominio.ModalidadeEntrega;

import java.math.BigDecimal;

/**
 * Comportamento de uma modalidade de entrega. Cada modalidade tem fórmula de custo,
 * prazo e regra de disponibilidade próprios (o que muda de caso para caso), por isso
 * cada uma mora numa implementação isolada, escolhida por lookup num mapa (ver
 * {@link EntregaRegistry}) — nunca por uma cadeia de if/else.
 *
 * A assinatura considera o caso mais exigente (MOTOBOY, cuja disponibilidade depende do
 * peso total do pedido).
 */
public interface EntregaCalculadora {

    ModalidadeEntrega getTipo();

    boolean disponivel(BigDecimal pesoTotalKg);

    BigDecimal calcularCustoBruto(BigDecimal pesoTotalKg);

    int getPrazoDias();
}
