package com.loja.checkout.cupom;

import com.loja.checkout.ItemPedido;

import java.math.BigDecimal;
import java.util.List;

public interface Cupom {

    String codigo();

    boolean isAplicavel(BigDecimal subtotalProdutos, List<ItemPedido> itens);

    BigDecimal calcularDesconto(BigDecimal subtotalProdutos, List<ItemPedido> itens, BigDecimal frete);
}
