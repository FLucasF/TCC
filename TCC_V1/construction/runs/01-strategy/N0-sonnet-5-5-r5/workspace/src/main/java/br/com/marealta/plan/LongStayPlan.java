package br.com.marealta.plan;

import br.com.marealta.ApiException;

/** Estadia longa: 7 noites ou mais, 10% de desconto, reembolso integral. */
public class LongStayPlan extends PercentagePlan {
    public static final int MIN_NIGHTS = 7;

    public LongStayPlan() {
        super("LONG_STAY", 10, true);
    }

    @Override
    public void validate(Booking booking) {
        if (booking.nights() < MIN_NIGHTS) {
            throw ApiException.invalid("O plano LONG_STAY exige " + MIN_NIGHTS + " noites ou mais.");
        }
    }
}
