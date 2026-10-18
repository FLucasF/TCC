package com.loja.service.club;

import com.loja.domain.ClubTier;
import com.loja.domain.Money;
import com.loja.domain.ShippingMethod;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;

@Component
public class SilverClub implements ClubTierStrategy {

    @Override
    public ClubTier getTier() {
        return ClubTier.SILVER;
    }

    @Override
    public Money calculateCredit(Money productsSubtotal) {
        return productsSubtotal.percentage(BigDecimal.valueOf(2));
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
