package com.loja.checkout.cupom;

import com.loja.checkout.api.ItemRequest;

import java.math.BigDecimal;
import java.util.List;

public interface Cupom {
    String codigo();
    boolean aplicavel(BigDecimal subtotalProdutos, List<ItemRequest> itens);
    BigDecimal calcularDesconto(BigDecimal subtotalProdutos, BigDecimal frete, List<ItemRequest> itens);
}
