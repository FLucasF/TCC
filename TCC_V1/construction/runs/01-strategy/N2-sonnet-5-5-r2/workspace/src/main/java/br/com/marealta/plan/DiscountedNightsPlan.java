package br.com.marealta.plan;

import br.com.marealta.domain.Money;
import br.com.marealta.domain.Quote;
import br.com.marealta.domain.Stay;

import java.math.BigDecimal;

/** Planos que cobram as noites de tabela e abatem um percentual da soma. Cada plano define só o que é seu. */
abstract class DiscountedNightsPlan implements RatePlan {
    private final String code;

    DiscountedNightsPlan(String code) {
        this.code = code;
    }

    @Override
    public final String code() {
        return code;
    }

    @Override
    public final Quote quote(Stay stay) {
        checkEligible(stay);
        BigDecimal subtotal = stay.sumOfNights();
        BigDecimal discount = Money.fraction(subtotal, discountRate(stay));
        return new Quote(stay.nights(), subtotal, discount);
    }

    /** Lança INVALID_REQUEST se a estadia não pode usar o plano. */
    void checkEligible(Stay stay) {
    }

    /** Fração de desconto sobre a soma das noites (0.15 = 15%). */
    abstract BigDecimal discountRate(Stay stay);
}
