package shop.shipping;

import shop.Money;

public final class StorePickupShipping implements ShippingOption {
    @Override
    public String code() {
        return "STORE_PICKUP";
    }

    @Override
    public ShippingQuote quote(ShippingContext context) {
        return new ShippingQuote(Money.ZERO, 1);
    }

    @Override
    public boolean hasShipping() {
        return false;
    }
}
