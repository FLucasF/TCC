package com.loja.checkout.cupom;

import com.loja.checkout.dto.ItemRequest;

import java.math.BigDecimal;
import java.util.List;

public interface Cupom {

    String codigo();

    default boolean aplicavel(BigDecimal subtotalProdutos, List<ItemRequest> itens) {
        return true;
    }

    BigDecimal calcularDesconto(BigDecimal subtotalProdutos, List<ItemRequest> itens, BigDecimal frete);
}
