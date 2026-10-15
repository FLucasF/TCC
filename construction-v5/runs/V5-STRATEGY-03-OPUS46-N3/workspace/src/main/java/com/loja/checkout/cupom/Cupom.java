package com.loja.checkout.cupom;

import com.loja.checkout.dto.ItemRequest;

import java.math.BigDecimal;
import java.util.List;

public interface Cupom {

    String getCodigo();

    boolean isAplicavel(BigDecimal subtotalProdutos, List<ItemRequest> itens);

    BigDecimal calcularDesconto(BigDecimal subtotalProdutos, List<ItemRequest> itens, BigDecimal frete);
}
