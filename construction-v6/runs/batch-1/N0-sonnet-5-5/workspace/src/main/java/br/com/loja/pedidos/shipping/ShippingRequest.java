package br.com.loja.pedidos.shipping;

import br.com.loja.pedidos.checkout.Region;
import java.math.BigDecimal;

public record ShippingRequest(String zipCode, BigDecimal weightKg, Region region) {
}
