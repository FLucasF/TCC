package marealta;

import java.math.BigDecimal;

public class LongStayPlan implements RatePlan {
    private static final int MIN_NIGHTS = 7;

    public String code() { return "LONG_STAY"; }

    @Override
    public void validate(Stay stay) {
        if (stay.nights() < MIN_NIGHTS) {
            throw ApiException.invalid("LONG_STAY exige " + MIN_NIGHTS + " noites ou mais");
        }
    }

    @Override
    public Quote quote(Stay stay, BigDecimal nightsTotal) {
        return new Quote(nightsTotal, Money.percent(nightsTotal, 10));
    }

    public BigDecimal refund(BigDecimal amountPaid) { return amountPaid; }
}
