package br.com.marealta.domain.rateplan;

import br.com.marealta.domain.DateRange;
import br.com.marealta.domain.Money;
import br.com.marealta.domain.RoomType;

import java.util.Map;

public class WeekendPackageRatePlan implements RatePlan {

    private static final Map<RoomType, Money> PACKAGE_PRICES = Map.of(
        RoomType.STANDARD, Money.of("520.00"),
        RoomType.DELUXE, Money.of("820.00"),
        RoomType.SUITE, Money.of("1200.00")
    );

    @Override
    public void validate(RoomType roomType, DateRange dateRange, int guests, String companyCode) {
        if (!dateRange.isFridayToSunday()) {
            throw new IllegalArgumentException("Weekend package requires Friday check-in and Sunday check-out (2 nights)");
        }
    }

    @Override
    public Money calculateSubtotal(RoomType roomType, DateRange dateRange) {
        return PACKAGE_PRICES.get(roomType);
    }

    @Override
    public Money calculateDiscount(Money subtotal) {
        return Money.zero();
    }

    @Override
    public Money calculateRefund(Money amountPaid) {
        return amountPaid.multiply(0.5);
    }

    @Override
    public String getType() {
        return "WEEKEND_PACKAGE";
    }
}
