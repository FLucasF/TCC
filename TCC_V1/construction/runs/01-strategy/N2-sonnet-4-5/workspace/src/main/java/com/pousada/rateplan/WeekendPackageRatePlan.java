package com.pousada.rateplan;

import com.pousada.domain.RoomType;
import com.pousada.exception.BusinessException;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.util.Map;

public class WeekendPackageRatePlan extends BaseRatePlanStrategy {

    private static final Map<RoomType, BigDecimal> PACKAGE_PRICES = Map.of(
        RoomType.STANDARD, new BigDecimal("520.00"),
        RoomType.DELUXE, new BigDecimal("820.00"),
        RoomType.SUITE, new BigDecimal("1200.00")
    );

    @Override
    public void validate(PricingContext context) {
        if (context.getNights() != 2) {
            throw new BusinessException("INVALID_REQUEST",
                "Weekend package requires exactly 2 nights", 400);
        }

        if (context.getCheckIn().getDayOfWeek() != DayOfWeek.FRIDAY) {
            throw new BusinessException("INVALID_REQUEST",
                "Weekend package must start on Friday", 400);
        }

        if (context.getCheckOut().getDayOfWeek() != DayOfWeek.SUNDAY) {
            throw new BusinessException("INVALID_REQUEST",
                "Weekend package must end on Sunday", 400);
        }
    }

    @Override
    public PricingResult calculatePrice(PricingContext context) {
        BigDecimal packagePrice = PACKAGE_PRICES.get(context.getRoomType());
        BigDecimal discount = BigDecimal.ZERO;

        return new PricingResult(packagePrice, discount, packagePrice);
    }

    @Override
    public BigDecimal calculateRefund(BigDecimal amountPaid) {
        return round(amountPaid.multiply(new BigDecimal("0.50")));
    }
}
