package br.com.loja.shipping;

import org.springframework.stereotype.Component;

import br.com.loja.common.Money;

@Component
class StorePickupShipping implements ShippingOption {

    @Override
    public String code() {
        return "STORE_PICKUP";
    }

    @Override
    public ShippingQuote quote(ShippingRequest request) {
        return new ShippingQuote(Money.ZERO, 1);
    }

    @Override
    public boolean chargesFreight() {
        return false;
    }
}
