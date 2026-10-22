package br.com.marealta.domain.rateplan;

import br.com.marealta.domain.DateRange;
import br.com.marealta.domain.Money;
import br.com.marealta.domain.RoomType;

import java.math.BigDecimal;

public class LongStayRatePlan implements RatePlan {

    @Override
    public void validate(RoomType roomType, DateRange dateRange, int guests, String companyCode) {
        if (dateRange.nights() < 7) {
            throw new IllegalArgumentException("Long stay requires minimum 7 nights");
        }
    }

    @Override
    public Money calculateSubtotal(RoomType roomType, DateRange dateRange) {
        return dateRange.calculateNightlyTotal(roomType.getNightlyRate());
    }

    @Override
    public Money calculateDiscount(Money subtotal) {
        return subtotal.discount(BigDecimal.valueOf(10));
    }

    @Override
    public Money calculateRefund(Money amountPaid) {
        return amountPaid;
    }

    @Override
    public String getType() {
        return "LONG_STAY";
    }
}
