package com.loja.domain.strategies.club;

import com.loja.domain.entities.*;

public class SilverTier implements ClubTierStrategy {

    @Override
    public Money calculateCredit(Money productsSubtotal) {
        return productsSubtotal.multiply(0.02);
    }

    @Override
    public Money adjustShippingCost(Money shippingCost, ShippingMethod shippingMethod) {
        return shippingCost;
    }

    @Override
    public boolean hasGift(Money productsSubtotal) {
        return false;
    }

    @Override
    public ClubTier getTier() {
        return ClubTier.SILVER;
    }
}
