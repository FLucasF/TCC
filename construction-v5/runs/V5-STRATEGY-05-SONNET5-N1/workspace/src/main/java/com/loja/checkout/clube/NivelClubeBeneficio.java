package com.loja.checkout.clube;

import com.loja.checkout.dominio.NivelClube;

import java.math.BigDecimal;

/**
 * Comportamento de um nível de clube. Cada nível tem percentual de crédito, isenção de
 * frete e regra de brinde próprios, por isso cada um mora numa implementação isolada,
 * escolhida por lookup num mapa (ver {@link ClubeRegistry}). O enunciado avisa que
 * podem surgir novos níveis no futuro, então basta criar uma nova classe e registrá-la
 * como componente Spring — nada aqui muda.
 *
 * A assinatura considera o caso mais exigente (OURO, que isenta frete e tem brinde
 * condicionado ao subtotal).
 */
public interface NivelClubeBeneficio {

    NivelClube getTipo();

    BigDecimal calcularCreditoBruto(BigDecimal subtotalProdutos);

    boolean isentaFrete();

    boolean temBrinde(BigDecimal subtotalProdutos);
}
