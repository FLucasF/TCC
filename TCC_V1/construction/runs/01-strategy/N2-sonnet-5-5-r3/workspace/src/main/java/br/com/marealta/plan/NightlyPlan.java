package br.com.marealta.plan;

import br.com.marealta.domain.Money;
import br.com.marealta.domain.Stay;

import java.math.BigDecimal;

/** Sequência comum dos planos que cobram as diárias com um percentual de desconto. */
abstract class NightlyPlan implements RatePlan {

    @Override
    public final Quote quote(Stay stay) {
        validate(stay);
        BigDecimal subtotal = stay.nightsTotal();
        BigDecimal discount = Money.round(subtotal.multiply(discountRate(stay)));
        return new Quote(stay.nights(), subtotal, discount, subtotal.subtract(discount));
    }

    /** Lança INVALID_REQUEST se a estadia não puder usar o plano. */
    protected void validate(Stay stay) {}

    protected abstract BigDecimal discountRate(Stay stay);
}
