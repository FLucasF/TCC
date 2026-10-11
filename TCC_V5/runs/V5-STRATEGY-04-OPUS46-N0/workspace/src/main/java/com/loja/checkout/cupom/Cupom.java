package com.loja.checkout.cupom;

import com.loja.checkout.ItemRequest;

import java.math.BigDecimal;
import java.util.List;

public interface Cupom {

    String codigo();

    BigDecimal calcularDesconto(BigDecimal subtotalProdutos, List<ItemRequest> itens, BigDecimal frete);

    default boolean aplicavel(BigDecimal subtotalProdutos, List<ItemRequest> itens) {
        return true;
    }
}
