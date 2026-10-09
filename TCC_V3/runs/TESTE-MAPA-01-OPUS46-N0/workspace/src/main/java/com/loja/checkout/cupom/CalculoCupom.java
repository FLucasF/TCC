package com.loja.checkout.cupom;

import com.loja.checkout.dto.ItemRequest;
import java.math.BigDecimal;
import java.util.List;

public interface CalculoCupom {

    String codigo();

    default void validar(List<ItemRequest> itens, BigDecimal subtotal) {
    }

    BigDecimal calcularDesconto(List<ItemRequest> itens, BigDecimal subtotal, BigDecimal frete);
}
