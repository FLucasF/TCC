package com.loja.strategy;

public class GoldClub implements ClubBenefitsStrategy {
    @Override
    public Double getCreditPercentage() {
        return 0.05;
    }

    @Override
    public boolean freeShipping(String shippingMethod) {
        return !"COURIER".equals(shippingMethod) && !"STORE_PICKUP".equals(shippingMethod);
    }

    @Override
    public boolean giftForLargeOrder(Double productsSubtotal) {
        return productsSubtotal > 500.0;
    }
}
