package com.loja.service.club;

import com.loja.domain.ClubTier;
import com.loja.domain.Money;
import com.loja.domain.ShippingMethod;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;

@Component
public class GoldClub implements ClubTierStrategy {

    private static final Money GIFT_THRESHOLD = new Money("500.00");

    @Override
    public ClubTier getTier() {
        return ClubTier.GOLD;
    }

    @Override
    public Money calculateCredit(Money productsSubtotal) {
        return productsSubtotal.percentage(BigDecimal.valueOf(5));
    }

    @Override
    public boolean hasFreeShipping(ShippingMethod shippingMethod) {
        return shippingMethod != ShippingMethod.COURIER;
    }

    @Override
    public boolean hasGift(Money productsSubtotal) {
        return productsSubtotal.isGreaterThan(GIFT_THRESHOLD);
    }
}
