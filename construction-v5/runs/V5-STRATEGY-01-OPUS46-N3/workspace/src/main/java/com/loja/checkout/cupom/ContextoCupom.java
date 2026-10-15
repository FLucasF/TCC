package com.loja.checkout.cupom;

import com.loja.checkout.ItemCarrinho;

import java.math.BigDecimal;
import java.util.List;

public record ContextoCupom(
        List<ItemCarrinho> itens,
        BigDecimal subtotal,
        BigDecimal frete
) {}
