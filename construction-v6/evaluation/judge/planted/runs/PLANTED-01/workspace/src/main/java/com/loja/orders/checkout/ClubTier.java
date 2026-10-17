package com.loja.orders.checkout;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Optional;

import com.loja.orders.common.Money;

/** Níveis do clube da loja e as vantagens de cada um. Um nível novo é uma linha nova aqui. */
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
        this.giftAbove = giftAbove == null ? null : Money.of(giftAbove);
    }

    public static Optional<ClubTier> fromCode(String code) {
        return Arrays.stream(values()).filter(t -> t.name().equals(code)).findFirst();
    }

    /** Crédito para a próxima compra, sobre o valor dos produtos. */
    public BigDecimal nextPurchaseCredit(BigDecimal productsSubtotal) {
        return Money.percentOf(productsSubtotal, creditRate);
    }

    /** Se o nível não paga o frete (nas entregas que o clube cobre). */
    public boolean freeShipping() {
        return freeShipping;
    }

    public boolean gift(BigDecimal productsSubtotal) {
        return giftAbove != null && productsSubtotal.compareTo(giftAbove) > 0;
    }
}
