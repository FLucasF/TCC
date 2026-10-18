package shop.domain;

import java.math.BigDecimal;
import shop.domain.shipping.ShippingOption;

/** Cada nível do clube é uma constante: um novo nível entra aqui e em mais nenhum lugar. */
public enum ClubTier {
    BRONZE("0", false, null),
    SILVER("2", false, null),
    GOLD("5", true, "500.00");

    private final BigDecimal creditPercent;
    private final boolean freeShipping;
    private final BigDecimal giftAbove;

    ClubTier(String creditPercent, boolean freeShipping, String giftAbove) {
        this.creditPercent = new BigDecimal(creditPercent);
        this.freeShipping = freeShipping;
        this.giftAbove = giftAbove == null ? null : new BigDecimal(giftAbove);
    }

    public static ClubTier parse(String value) {
        try {
            return ClubTier.valueOf(value);
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new DomainException(ErrorCode.INVALID_CLUB_TIER);
        }
    }

    public BigDecimal credit(BigDecimal productsSubtotal) {
        return Money.percent(productsSubtotal, creditPercent);
    }

    public boolean waivesShipping(ShippingOption option) {
        return freeShipping && option.clubWaivable();
    }

    public boolean gift(BigDecimal productsSubtotal) {
        return giftAbove != null && productsSubtotal.compareTo(giftAbove) > 0;
    }
}
