package br.com.marealta.plan;

import br.com.marealta.ApiError;
import br.com.marealta.Money;
import br.com.marealta.Stay;
import java.math.BigDecimal;

public class LongStayPlan extends RatePlan {
    private static final int MIN_NIGHTS = 7;
    private static final BigDecimal DISCOUNT = new BigDecimal("0.10");

    public LongStayPlan() {
        super("LONG_STAY");
    }

    @Override
    protected void validate(Stay stay) {
        if (stay.nights() < MIN_NIGHTS) {
            throw ApiError.invalidRequest("A estadia longa exige " + MIN_NIGHTS + " noites ou mais");
        }
    }

    @Override
    protected BigDecimal discount(Stay stay, BigDecimal subtotal) {
        return Money.fraction(subtotal, DISCOUNT);
    }
}
