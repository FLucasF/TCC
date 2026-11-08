package br.com.marealta.plan;

import br.com.marealta.domain.BusinessException;
import br.com.marealta.domain.Stay;

import java.math.BigDecimal;

final class LongStayPlan extends DiscountedNightsPlan {
    private static final int MIN_NIGHTS = 7;

    LongStayPlan() {
        super("LONG_STAY");
    }

    @Override
    void checkEligible(Stay stay) {
        if (stay.nights() < MIN_NIGHTS) {
            throw BusinessException.invalid("O plano LONG_STAY exige pelo menos " + MIN_NIGHTS + " noites");
        }
    }

    @Override
    BigDecimal discountRate(Stay stay) {
        return new BigDecimal("0.10");
    }

    @Override
    public BigDecimal refund(BigDecimal amountPaid) {
        return amountPaid;
    }
}
