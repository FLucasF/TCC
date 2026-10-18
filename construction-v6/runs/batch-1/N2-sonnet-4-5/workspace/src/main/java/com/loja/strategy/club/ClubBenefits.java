package com.loja.strategy.club;

import java.math.BigDecimal;

public class ClubBenefits {
    private final BigDecimal creditPercentage;
    private final boolean freeShipping;
    private final BigDecimal giftThreshold;

    public ClubBenefits(BigDecimal creditPercentage, boolean freeShipping, BigDecimal giftThreshold) {
        this.creditPercentage = creditPercentage;
        this.freeShipping = freeShipping;
        this.giftThreshold = giftThreshold;
    }

    public BigDecimal getCreditPercentage() { return creditPercentage; }
    public boolean isFreeShipping() { return freeShipping; }
    public BigDecimal getGiftThreshold() { return giftThreshold; }
}
