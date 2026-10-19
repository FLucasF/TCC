package marealta;

import java.math.BigDecimal;

public class NonRefundablePlan implements RatePlan {
    public String code() { return "NON_REFUNDABLE"; }

    @Override
    public Quote quote(Stay stay, BigDecimal nightsTotal) {
        return new Quote(nightsTotal, Money.percent(nightsTotal, 15));
    }

    public BigDecimal refund(BigDecimal amountPaid) { return Money.ZERO; }
}
