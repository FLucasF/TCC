package br.com.marealta.domain.rateplan;

import br.com.marealta.domain.DateRange;
import br.com.marealta.domain.Money;
import br.com.marealta.domain.RoomType;

public interface RatePlan {

    void validate(RoomType roomType, DateRange dateRange, int guests, String companyCode);

    Money calculateSubtotal(RoomType roomType, DateRange dateRange);

    Money calculateDiscount(Money subtotal);

    Money calculateRefund(Money amountPaid);

    String getType();
}
