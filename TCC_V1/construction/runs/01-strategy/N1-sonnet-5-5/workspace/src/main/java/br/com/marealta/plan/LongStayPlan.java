package br.com.marealta.plan;

import br.com.marealta.domain.ApiException;
import br.com.marealta.domain.Stay;
import java.math.BigDecimal;

class LongStayPlan extends DiscountPlan {

    private static final int MINIMUM_NIGHTS = 7;

    @Override
    public String code() {
        return "LONG_STAY";
    }

    @Override
    protected BigDecimal discountRate(Stay stay) {
        if (stay.nights() < MINIMUM_NIGHTS) {
            throw ApiException.invalidRequest("O plano LONG_STAY exige " + MINIMUM_NIGHTS + " noites ou mais");
        }
        return percent("10");
    }

    @Override
    public BigDecimal refund(BigDecimal amountPaid) {
        return amountPaid;
    }
}
