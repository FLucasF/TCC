package br.com.marealta.plan;

import br.com.marealta.Money;

import java.math.BigDecimal;

/** Plano com desconto percentual sobre a soma das diárias. */
public class PercentagePlan implements RatePlan {
    private final String code;
    private final int discountPercent;
    private final boolean refundable;

    public PercentagePlan(String code, int discountPercent, boolean refundable) {
        this.code = code;
        this.discountPercent = discountPercent;
        this.refundable = refundable;
    }

    @Override
    public String code() {
        return code;
    }

    protected int discountPercent(Booking booking) {
        return discountPercent;
    }

    @Override
    public Price price(Booking booking, BigDecimal nightsSum) {
        return new Price(nightsSum, Money.percent(nightsSum, discountPercent(booking)));
    }

    @Override
    public BigDecimal refund(BigDecimal amountPaid) {
        return refundable ? amountPaid : Money.ZERO;
    }
}
