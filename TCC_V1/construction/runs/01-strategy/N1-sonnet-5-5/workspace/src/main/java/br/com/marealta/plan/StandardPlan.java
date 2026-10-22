package br.com.marealta.plan;

import br.com.marealta.domain.Stay;
import java.math.BigDecimal;

class StandardPlan extends DiscountPlan {

    @Override
    public String code() {
        return "STANDARD";
    }

    @Override
    protected BigDecimal discountRate(Stay stay) {
        return BigDecimal.ZERO;
    }

    @Override
    public BigDecimal refund(BigDecimal amountPaid) {
        return amountPaid;
    }
}
