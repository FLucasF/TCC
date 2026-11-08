package br.com.marealta.plan;

import java.math.BigDecimal;

import br.com.marealta.domain.DomainException;
import br.com.marealta.domain.Money;
import br.com.marealta.domain.Stay;

/** 7 noites ou mais: 10% de desconto sobre as diárias. */
public class LongStayPlan implements RatePlan {
    private static final int MIN_NIGHTS = 7;

    @Override
    public String code() {
        return "LONG_STAY";
    }

    @Override
    public void validate(Stay stay) {
        if (stay.nights() < MIN_NIGHTS) {
            throw DomainException.invalid("LONG_STAY exige no mínimo " + MIN_NIGHTS + " noites");
        }
    }

    @Override
    public BigDecimal discount(Stay stay, BigDecimal subtotal) {
        return Money.percent(subtotal, "0.10");
    }
}
