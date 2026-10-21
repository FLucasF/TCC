package pousada.plan;

import pousada.Money;
import pousada.Quote;
import pousada.Stay;

import java.math.BigDecimal;

/** Plans that charge the nights and take a percentage off the sum; each plan chooses the percentage. */
abstract class DiscountedPlan implements RatePlan {
    /** Fraction taken off the sum of the nights, e.g. "0.15". */
    protected abstract String discountFraction(Stay stay);

    @Override
    public void validate(Stay stay) {}

    @Override
    public Quote quote(Stay stay) {
        BigDecimal subtotal = stay.nightsTotal();
        return new Quote(stay.nights(), subtotal, Money.fraction(subtotal, discountFraction(stay)));
    }
}
