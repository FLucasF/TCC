package br.com.loja.shipping;

import java.math.BigDecimal;

public record ShippingRequest(String zipCode, Region region, BigDecimal weightKg) {
}
