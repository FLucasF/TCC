package com.loja.checkout.cupom;

import com.loja.checkout.ItemRequest;

import java.math.BigDecimal;
import java.util.List;

public interface CalculoCupom {

    String getCodigo();

    BigDecimal calcularDesconto(BigDecimal subtotalProdutos, List<ItemRequest> itens, BigDecimal frete);

    boolean isAplicavel(BigDecimal subtotalProdutos, List<ItemRequest> itens);
}
