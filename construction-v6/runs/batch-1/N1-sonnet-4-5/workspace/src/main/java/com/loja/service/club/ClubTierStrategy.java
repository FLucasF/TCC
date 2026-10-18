package com.loja.service.club;

import com.loja.domain.ClubTier;
import com.loja.domain.Money;
import com.loja.domain.ShippingMethod;

public interface ClubTierStrategy {
    ClubTier getTier();

    Money calculateCredit(Money productsSubtotal);

    boolean hasFreeShipping(ShippingMethod shippingMethod);

    boolean hasGift(Money productsSubtotal);
}
