package br.com.marealta.plan;

import br.com.marealta.domain.Money;
import br.com.marealta.domain.Stay;
import java.math.BigDecimal;

class NonRefundablePlan extends DiscountPlan {

    @Override
    public String code() {
        return "NON_REFUNDABLE";
    }

    @Override
    protected BigDecimal discountRate(Stay stay) {
        return percent("15");
    }

    @Override
    public BigDecimal refund(BigDecimal amountPaid) {
        return Money.ZERO;
    }
}
