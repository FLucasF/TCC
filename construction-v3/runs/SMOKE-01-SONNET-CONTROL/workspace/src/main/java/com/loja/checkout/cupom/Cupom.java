package com.loja.checkout.cupom;

import com.loja.checkout.dto.ItemRequest;
import com.loja.checkout.exception.CheckoutException;

import java.math.BigDecimal;
import java.util.List;

/**
 * Cada cupom é implementado como um componente Spring cujo nome de bean é
 * exatamente o código do cupom (ex.: "BEMVINDO10"), permitindo criar novas
 * promoções sem alterar o serviço de cálculo.
 */
public interface Cupom {

    default void validarAplicabilidade(BigDecimal subtotalProdutos, List<ItemRequest> itens) {
        // por padrão todo cupom se aplica a qualquer pedido
    }

    BigDecimal calcularDesconto(BigDecimal subtotalProdutos, BigDecimal frete, List<ItemRequest> itens);

    default void naoAplicavel() {
        throw new CheckoutException("CUPOM_NAO_APLICAVEL");
    }
}
