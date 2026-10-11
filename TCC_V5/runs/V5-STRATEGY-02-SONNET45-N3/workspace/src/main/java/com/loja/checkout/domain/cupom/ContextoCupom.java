package com.loja.checkout.domain.cupom;

import com.loja.checkout.dto.ItemPedido;
import java.math.BigDecimal;
import java.util.List;

public record ContextoCupom(
    BigDecimal subtotalProdutos,
    BigDecimal frete,
    List<ItemPedido> itens
) {}
