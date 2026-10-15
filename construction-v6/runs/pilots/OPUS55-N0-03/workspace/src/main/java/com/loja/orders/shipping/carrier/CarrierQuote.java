package com.loja.orders.shipping.carrier;

import java.math.BigDecimal;

public record CarrierQuote(BigDecimal price, int etaHours) {
}
