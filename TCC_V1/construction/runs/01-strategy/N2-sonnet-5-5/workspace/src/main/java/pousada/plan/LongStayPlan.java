package pousada.plan;

import pousada.PousadaException;
import pousada.Stay;

import java.math.BigDecimal;

public class LongStayPlan extends DiscountedPlan {
    private static final int MIN_NIGHTS = 7;

    public String code() { return "LONG_STAY"; }

    @Override
    public void validate(Stay stay) {
        if (stay.nights() < MIN_NIGHTS) {
            throw PousadaException.invalid("O plano LONG_STAY exige no mínimo " + MIN_NIGHTS + " noites");
        }
    }

    protected String discountFraction(Stay stay) { return "0.10"; }

    public BigDecimal refund(BigDecimal amountPaid) { return amountPaid; }
}
