package com.loja.service.club;

import com.loja.domain.ClubTier;
import com.loja.domain.Money;
import com.loja.domain.ShippingMethod;
import org.springframework.stereotype.Component;

@Component
public class BronzeClub implements ClubTierStrategy {

    @Override
    public ClubTier getTier() {
        return ClubTier.BRONZE;
    }

    @Override
    public Money calculateCredit(Money productsSubtotal) {
        return Money.zero();
    }

    @Override
    public boolean hasFreeShipping(ShippingMethod shippingMethod) {
        return false;
    }

    @Override
    public boolean hasGift(Money productsSubtotal) {
        return false;
    }
}
