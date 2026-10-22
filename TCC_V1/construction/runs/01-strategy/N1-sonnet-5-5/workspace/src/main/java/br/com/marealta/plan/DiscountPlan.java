package br.com.marealta.plan;

import br.com.marealta.domain.Money;
import br.com.marealta.domain.Pricing;
import br.com.marealta.domain.Stay;
import java.math.BigDecimal;

/** Base dos planos que cobram as diárias normais e abatem um percentual da soma delas. */
abstract class DiscountPlan implements RatePlan {

    /** Percentual de desconto (0.15 = 15%); lança INVALID_REQUEST se a estadia não puder usar o plano. */
    protected abstract BigDecimal discountRate(Stay stay);

    @Override
    public Pricing quote(Stay stay) {
        BigDecimal rate = discountRate(stay);
        BigDecimal subtotal = stay.nightsTotal();
        return new Pricing(subtotal, Money.round(subtotal.multiply(rate)));
    }

    protected static BigDecimal percent(String value) {
        return new BigDecimal(value).movePointLeft(2);
    }
}
