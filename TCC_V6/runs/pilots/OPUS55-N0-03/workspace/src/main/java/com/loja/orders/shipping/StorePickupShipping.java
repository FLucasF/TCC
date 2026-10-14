package com.loja.orders.shipping;

import org.springframework.stereotype.Component;

import com.loja.orders.common.Money;

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
    public boolean hasShippingFee() {
        return false;
    }
}
