package pousada.plan;

import pousada.domain.AppException;
import pousada.domain.ErrorCode;
import pousada.domain.Money;

import java.math.BigDecimal;

public class LongStayPlan implements RatePlan {
    private static final int MIN_NIGHTS = 7;

    @Override
    public String code() {
        return "LONG_STAY";
    }

    @Override
    public void validate(Stay stay) {
        if (stay.nights() < MIN_NIGHTS) {
            throw new AppException(ErrorCode.INVALID_REQUEST,
                    "O plano LONG_STAY exige " + MIN_NIGHTS + " noites ou mais");
        }
    }

    @Override
    public BigDecimal discount(Stay stay, BigDecimal subtotal) {
        return Money.fraction(subtotal, "0.10");
    }

    @Override
    public BigDecimal refund(BigDecimal amountPaid) {
        return amountPaid;
    }
}
