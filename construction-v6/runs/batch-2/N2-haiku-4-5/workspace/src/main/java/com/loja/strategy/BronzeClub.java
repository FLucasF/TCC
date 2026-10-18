package com.loja.strategy;

public class BronzeClub implements ClubBenefitsStrategy {
    @Override
    public Double getCreditPercentage() {
        return 0.0;
    }

    @Override
    public boolean freeShipping(String shippingMethod) {
        return false;
    }

    @Override
    public boolean giftForLargeOrder(Double productsSubtotal) {
        return false;
    }
}
