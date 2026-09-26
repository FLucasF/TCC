package com.loja.checkout.cupom;

import com.loja.checkout.dto.ItemRequest;

import java.math.BigDecimal;
import java.util.List;

public record CupomContexto(BigDecimal subtotalProdutos, List<ItemRequest> itens, BigDecimal frete) {
}
