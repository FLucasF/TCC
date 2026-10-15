package com.loja.checkout.domain.cupom;

import com.loja.checkout.domain.PedidoRequest;

import java.math.BigDecimal;
import java.util.List;

public record ContextoCupom(
    BigDecimal subtotalProdutos,
    List<PedidoRequest.ItemCarrinho> itens,
    BigDecimal frete
) {}
