package br.com.loja.pedidos.shipping;

import java.math.BigDecimal;

import br.com.loja.pedidos.common.Region;

public record ShippingRequest(String zipCode, Region region, BigDecimal weightKg) {
}
