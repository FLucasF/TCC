package com.loja.checkout.cupom;

import java.math.BigDecimal;
import java.util.List;

import com.loja.checkout.ItemRequest;

public interface Cupom {

    String codigo();

    boolean aplicavel(BigDecimal subtotalProdutos, List<ItemRequest> itens);

    BigDecimal calcularDesconto(BigDecimal subtotalProdutos, BigDecimal frete, List<ItemRequest> itens);
}
