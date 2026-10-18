package shop.domain;

import java.math.BigDecimal;

/** A club level and its perks. New levels are new instances registered in {@link Rules}. */
public record ClubTier(String code, BigDecimal creditRate, boolean freeShipping, BigDecimal giftAbove) {

    public BigDecimal credit(BigDecimal productsSubtotal) {
        return Money.round(productsSubtotal.multiply(creditRate));
    }

    public boolean gift(BigDecimal productsSubtotal) {
        return giftAbove != null && productsSubtotal.compareTo(giftAbove) > 0;
    }
}
