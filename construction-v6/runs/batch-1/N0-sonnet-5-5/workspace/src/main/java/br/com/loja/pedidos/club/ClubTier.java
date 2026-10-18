package br.com.loja.pedidos.club;

import br.com.loja.pedidos.common.Money;
import java.math.BigDecimal;
import java.util.Optional;

/** Club level and its perks. A new level is a new constant with its own set of perks. */
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
        return Money.percent(productsSubtotal, creditRate);
    }

    public boolean freeShipping() {
        return freeShipping;
    }

    public boolean gift(BigDecimal productsSubtotal) {
        return giftAbove != null && productsSubtotal.compareTo(giftAbove) > 0;
    }

    public static Optional<ClubTier> parse(String code) {
        for (ClubTier tier : values()) {
            if (tier.name().equals(code)) {
                return Optional.of(tier);
            }
        }
        return Optional.empty();
    }
}
