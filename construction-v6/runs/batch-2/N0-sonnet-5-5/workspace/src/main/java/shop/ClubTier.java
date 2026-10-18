package shop;

import java.math.BigDecimal;

/** Cada nível do clube define suas vantagens; um novo nível é uma nova constante. */
public enum ClubTier {
    BRONZE("0", false, null),
    SILVER("0.02", false, null),
    GOLD("0.05", true, "500.00");

    private final String creditRate;
    private final boolean freeShipping;
    private final BigDecimal giftAbove;

    ClubTier(String creditRate, boolean freeShipping, String giftAbove) {
        this.creditRate = creditRate;
        this.freeShipping = freeShipping;
        this.giftAbove = giftAbove == null ? null : new BigDecimal(giftAbove);
    }

    public BigDecimal credit(BigDecimal productsSubtotal) {
        return Money.percent(productsSubtotal, creditRate);
    }

    public boolean waivesShipping() {
        return freeShipping;
    }

    public boolean gift(BigDecimal productsSubtotal) {
        return giftAbove != null && productsSubtotal.compareTo(giftAbove) > 0;
    }

    public static ClubTier parse(String code) {
        if (code == null) return null;
        try {
            return valueOf(code);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
