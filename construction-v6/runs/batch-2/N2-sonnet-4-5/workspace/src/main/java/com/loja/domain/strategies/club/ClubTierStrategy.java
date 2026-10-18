package com.loja.domain.strategies.club;

import com.loja.domain.entities.*;

public interface ClubTierStrategy {
    Money calculateCredit(Money productsSubtotal);

    Money adjustShippingCost(Money shippingCost, ShippingMethod shippingMethod);

    boolean hasGift(Money productsSubtotal);

    ClubTier getTier();
}
