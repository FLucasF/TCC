package br.com.marealta.domain.rateplan;

import br.com.marealta.domain.DateRange;
import br.com.marealta.domain.Money;
import br.com.marealta.domain.RoomType;

import java.math.BigDecimal;

public class NonRefundableRatePlan implements RatePlan {

    @Override
    public void validate(RoomType roomType, DateRange dateRange, int guests, String companyCode) {
    }

    @Override
    public Money calculateSubtotal(RoomType roomType, DateRange dateRange) {
        return dateRange.calculateNightlyTotal(roomType.getNightlyRate());
    }

    @Override
    public Money calculateDiscount(Money subtotal) {
        return subtotal.discount(BigDecimal.valueOf(15));
    }

    @Override
    public Money calculateRefund(Money amountPaid) {
        return Money.zero();
    }

    @Override
    public String getType() {
        return "NON_REFUNDABLE";
    }
}
