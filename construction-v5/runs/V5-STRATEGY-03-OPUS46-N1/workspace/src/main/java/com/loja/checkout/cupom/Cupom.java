package com.loja.checkout.cupom;

import java.math.BigDecimal;
import java.util.List;

import com.loja.checkout.dto.ItemRequest;

public interface Cupom {

    String getCodigo();

    BigDecimal calcularDesconto(BigDecimal subtotalProdutos, BigDecimal frete, List<ItemRequest> itens);

    boolean isAplicavel(BigDecimal subtotalProdutos);
}
