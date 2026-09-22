package com.loja.checkout.cupom;

import com.loja.checkout.dto.ItemPedido;
import java.math.BigDecimal;
import java.util.List;

public record ContextoDesconto(List<ItemPedido> itens, BigDecimal subtotalProdutos, BigDecimal frete) {
}
