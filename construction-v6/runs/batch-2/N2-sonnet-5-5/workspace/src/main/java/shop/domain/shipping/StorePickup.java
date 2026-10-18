package shop.domain.shipping;

import shop.domain.Money;

public class StorePickup implements ShippingOption {
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

    @Override
    public boolean clubWaivable() {
        return true;
    }
}
