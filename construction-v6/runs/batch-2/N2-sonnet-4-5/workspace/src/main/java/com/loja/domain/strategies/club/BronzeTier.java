package com.loja.domain.strategies.club;

import com.loja.domain.entities.*;

public class BronzeTier implements ClubTierStrategy {

    @Override
    public Money calculateCredit(Money productsSubtotal) {
        return Money.zero();
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
        return ClubTier.BRONZE;
    }
}
