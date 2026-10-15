package com.loja.checkout.domain.cupom;

import com.loja.checkout.dto.ItemRequest;

import java.math.BigDecimal;
import java.util.List;

public interface Cupom {

    boolean aplicavel(BigDecimal subtotalProdutos);

    BigDecimal calcularDesconto(BigDecimal subtotalProdutos, BigDecimal frete, List<ItemRequest> itens);
}
