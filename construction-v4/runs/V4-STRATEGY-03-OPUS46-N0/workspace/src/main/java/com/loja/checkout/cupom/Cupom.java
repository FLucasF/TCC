package com.loja.checkout.cupom;

import com.loja.checkout.dto.ItemRequest;

import java.math.BigDecimal;
import java.util.List;

public interface Cupom {

    String codigo();

    default boolean aplicavel(List<ItemRequest> itens, BigDecimal subtotal) {
        return true;
    }

    BigDecimal calcularDesconto(List<ItemRequest> itens, BigDecimal subtotal, BigDecimal frete);
}
