package com.loja.domain.strategies.club;

import com.loja.domain.entities.*;

public class GoldTier implements ClubTierStrategy {
    private static final Money GIFT_THRESHOLD = Money.of(500.00);

    @Override
    public Money calculateCredit(Money productsSubtotal) {
        return productsSubtotal.multiply(0.05);
    }

    @Override
    public Money adjustShippingCost(Money shippingCost, ShippingMethod shippingMethod) {
        if (shippingMethod == ShippingMethod.COURIER) {
            return shippingCost;
        }
        return Money.zero();
    }

    @Override
    public boolean hasGift(Money productsSubtotal) {
        return productsSubtotal.isGreaterThan(GIFT_THRESHOLD);
    }

    @Override
    public ClubTier getTier() {
        return ClubTier.GOLD;
    }
}
