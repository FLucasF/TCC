package com.loja.checkout.domain.cupom;

import com.loja.checkout.model.request.ItemRequest;

import java.math.BigDecimal;
import java.util.List;

public interface Cupom {
    boolean eAplicavel(BigDecimal subtotalProdutos, BigDecimal frete, List<ItemRequest> itens);
    BigDecimal calcularDesconto(BigDecimal subtotalProdutos, BigDecimal frete, List<ItemRequest> itens);
}
