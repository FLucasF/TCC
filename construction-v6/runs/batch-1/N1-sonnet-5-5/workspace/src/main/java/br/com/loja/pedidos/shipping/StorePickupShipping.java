package br.com.loja.pedidos.shipping;

import org.springframework.stereotype.Component;

import br.com.loja.pedidos.common.Money;

@Component
public class StorePickupShipping implements ShippingOption {

    @Override
    public String code() {
        return "STORE_PICKUP";
    }

    @Override
    public ShippingQuote quote(ShippingRequest request) {
        return new ShippingQuote(Money.ZERO, 1);
    }

    @Override
    public boolean chargesShipping() {
        return false;
    }
}
