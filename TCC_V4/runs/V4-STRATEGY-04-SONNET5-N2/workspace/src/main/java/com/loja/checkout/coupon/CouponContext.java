package com.loja.checkout.coupon;

import com.loja.checkout.dto.ItemRequest;

import java.math.BigDecimal;
import java.util.List;

public record CouponContext(List<ItemRequest> itens, BigDecimal subtotalProdutos, BigDecimal frete) {
}
