package com.loja.checkout.domain.cupom;

import com.loja.checkout.dto.ItemRequest;

import java.math.BigDecimal;
import java.util.List;

public interface Cupom {

    String getCodigo();

    BigDecimal calcularDesconto(BigDecimal subtotalProdutos, BigDecimal frete, List<ItemRequest> itens);

    boolean isAplicavel(BigDecimal subtotalProdutos, List<ItemRequest> itens);
}
