package br.com.marealta.domain.rateplan;

import br.com.marealta.domain.DateRange;
import br.com.marealta.domain.Money;
import br.com.marealta.domain.RoomType;

public class StandardRatePlan implements RatePlan {

    @Override
    public void validate(RoomType roomType, DateRange dateRange, int guests, String companyCode) {
    }

    @Override
    public Money calculateSubtotal(RoomType roomType, DateRange dateRange) {
        return dateRange.calculateNightlyTotal(roomType.getNightlyRate());
    }

    @Override
    public Money calculateDiscount(Money subtotal) {
        return Money.zero();
    }

    @Override
    public Money calculateRefund(Money amountPaid) {
        return amountPaid;
    }

    @Override
    public String getType() {
        return "STANDARD";
    }
}
