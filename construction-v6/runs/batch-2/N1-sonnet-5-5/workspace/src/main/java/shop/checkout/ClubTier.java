package shop.checkout;

import shop.Money;

import java.math.BigDecimal;

/** Club level and its benefits. A new level is a new constant with its own benefits. */
public enum ClubTier {
    BRONZE("0", false, null),
    SILVER("0.02", false, null),
    GOLD("0.05", true, "500.00");

    private final BigDecimal creditRate;
    private final boolean freeShipping;
    private final BigDecimal giftAbove;

    ClubTier(String creditRate, boolean freeShipping, String giftAbove) {
        this.creditRate = new BigDecimal(creditRate);
        this.freeShipping = freeShipping;
        this.giftAbove = giftAbove == null ? null : new BigDecimal(giftAbove);
    }

    public BigDecimal nextPurchaseCredit(BigDecimal productsSubtotal) {
        return Money.round(productsSubtotal.multiply(creditRate));
    }

    public boolean waivesShipping() {
        return freeShipping;
    }

    public boolean gift(BigDecimal productsSubtotal) {
        return giftAbove != null && productsSubtotal.compareTo(giftAbove) > 0;
    }
}
