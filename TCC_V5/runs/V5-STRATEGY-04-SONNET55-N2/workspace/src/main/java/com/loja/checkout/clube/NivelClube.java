package com.loja.checkout.clube;

import com.loja.checkout.Identificavel;
import java.math.BigDecimal;

public interface NivelClube extends Identificavel {
    /** Crédito para a próxima compra, sobre o valor dos produtos. */
    BigDecimal credito(BigDecimal subtotalProdutos);

    /** Frete que o cliente deste nível passa a ter, dado o frete da entrega escolhida. */
    BigDecimal frete(BigDecimal freteEntrega);

    boolean brinde(BigDecimal subtotalProdutos);
}
