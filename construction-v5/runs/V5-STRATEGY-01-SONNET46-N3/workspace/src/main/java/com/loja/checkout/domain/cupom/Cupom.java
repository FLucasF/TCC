package com.loja.checkout.domain.cupom;

import com.loja.checkout.web.ItemRequest;

import java.math.BigDecimal;
import java.util.List;

public interface Cupom {
    String codigo();
    boolean aplicavel(BigDecimal subtotal, List<ItemRequest> itens);
    BigDecimal calcularDesconto(BigDecimal subtotal, List<ItemRequest> itens, BigDecimal frete);
}
