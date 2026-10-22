package br.com.marealta.plan;

import br.com.marealta.ApiException;
import java.math.BigDecimal;

public class LongStayPlan extends AbstractRatePlan {
    private static final int MIN_NIGHTS = 7;

    public String code() { return "LONG_STAY"; }

    public Quote quote(QuoteRequest r) {
        if (r.nights() < MIN_NIGHTS) {
            throw ApiException.invalid("O LONG_STAY exige pelo menos " + MIN_NIGHTS + " noites");
        }
        return withDiscount(r, "0.10");
    }

    public BigDecimal refund(BigDecimal paid) { return full(paid); }
}
