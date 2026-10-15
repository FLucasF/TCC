package com.loja.resumo.cupom;

import com.loja.resumo.model.ItemPedido;
import java.math.BigDecimal;
import java.util.List;

public interface Cupom {

    String codigo();

    boolean aplicavel(List<ItemPedido> itens, BigDecimal subtotalProdutos);

    BigDecimal calcularDesconto(List<ItemPedido> itens, BigDecimal subtotalProdutos, BigDecimal frete);
}
