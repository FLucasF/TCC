package com.loja.checkout.resumo.clube;

import java.math.BigDecimal;

/**
 * Um nível do clube da loja, com seu conjunto de vantagens. Cada nível novo é
 * uma implementação desta interface registrada em {@link NiveisClube}.
 */
public interface NivelClube {

    /** Crédito para a próxima compra, sobre o valor dos produtos. */
    BigDecimal credito(BigDecimal subtotalProdutos);

    /** O frete que o cliente deste nível paga, a partir do frete calculado. */
    BigDecimal frete(BigDecimal freteCalculado);

    /** Se o pedido vai com brinde. */
    boolean brinde(BigDecimal subtotalProdutos);
}
