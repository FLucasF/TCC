package com.loja.orders.shipping;

import java.math.BigDecimal;

public record ShippingQuote(BigDecimal cost, int deliveryDays) {
}
