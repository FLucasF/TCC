package com.loja.checkout.domain.clube;

import java.math.BigDecimal;

/**
 * Um nível do clube da loja. Cada nível reúne num só lugar o que o cliente
 * ganha: crédito para a próxima compra, o que faz com o frete e se manda
 * brinde. Um nível novo entra como uma nova implementação.
 */
public interface NivelClube {

    /** Código exatamente como o site envia (ex.: "OURO"). */
    String codigo();

    /** Crédito para a próxima compra sobre o valor dos produtos, em centavos. */
    BigDecimal credito(BigDecimal subtotalProdutos);

    /** O frete que o cliente deste nível paga, a partir do frete da modalidade. */
    BigDecimal freteDevido(BigDecimal freteModalidade);

    /** Se o pedido vai com brinde. */
    boolean brinde(BigDecimal subtotalProdutos);
}
