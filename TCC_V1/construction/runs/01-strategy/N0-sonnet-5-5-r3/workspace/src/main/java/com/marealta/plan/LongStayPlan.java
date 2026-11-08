package com.marealta.plan;

import com.marealta.ApiException;

class LongStayPlan extends PerNightPlan {
    static final int MIN_NIGHTS = 7;

    @Override public String code() { return "LONG_STAY"; }

    @Override protected String discountRate(BookingTerms t) {
        if (t.nights() < MIN_NIGHTS) {
            throw ApiException.invalid("O LONG_STAY exige pelo menos " + MIN_NIGHTS + " noites");
        }
        return "0.10";
    }
}
