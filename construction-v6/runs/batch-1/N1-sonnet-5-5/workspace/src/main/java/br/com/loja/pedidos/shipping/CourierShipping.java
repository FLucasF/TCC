package br.com.loja.pedidos.shipping;

import java.math.BigDecimal;

import org.springframework.stereotype.Component;

import br.com.loja.pedidos.common.DomainException;
import br.com.loja.pedidos.common.Money;

@Component
public class CourierShipping implements ShippingOption {

    private static final BigDecimal MAX_WEIGHT_KG = new BigDecimal("5");
    private static final BigDecimal PRICE = new BigDecimal("18.00");

    @Override
    public String code() {
        return "COURIER";
    }

    @Override
    public ShippingQuote quote(ShippingRequest request) {
        if (request.weightKg().compareTo(MAX_WEIGHT_KG) > 0) {
            throw DomainException.unprocessable("SHIPPING_METHOD_UNAVAILABLE");
        }
        return new ShippingQuote(Money.round(PRICE), 0);
    }

    @Override
    public boolean clubFreeShippingApplies() {
        return false;
    }
}
