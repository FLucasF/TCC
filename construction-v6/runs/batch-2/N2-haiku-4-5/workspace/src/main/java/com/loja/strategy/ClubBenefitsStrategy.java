package com.loja.strategy;

public interface ClubBenefitsStrategy {
    Double getCreditPercentage();
    boolean freeShipping(String shippingMethod);
    boolean giftForLargeOrder(Double productsSubtotal);
}
