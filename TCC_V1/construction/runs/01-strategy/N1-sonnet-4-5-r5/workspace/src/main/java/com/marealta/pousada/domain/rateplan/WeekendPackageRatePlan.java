package com.marealta.pousada.domain.rateplan;

import com.marealta.pousada.domain.model.RoomType;
import com.marealta.pousada.exception.InvalidRequestException;
import com.marealta.pousada.util.DateUtil;
import com.marealta.pousada.util.MoneyUtil;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

public class WeekendPackageRatePlan extends AbstractRatePlan {

    private static final Map<RoomType, BigDecimal> PACKAGE_PRICES = Map.of(
        RoomType.STANDARD, new BigDecimal("520.00"),
        RoomType.DELUXE, new BigDecimal("820.00"),
        RoomType.SUITE, new BigDecimal("1200.00")
    );

    @Override
    public String getName() {
        return "WEEKEND_PACKAGE";
    }

    @Override
    public void validate(LocalDate checkIn, LocalDate checkOut, RoomType roomType, String companyCode) {
        long nights = countNights(checkIn, checkOut);

        if (nights != 2) {
            throw new InvalidRequestException("Weekend package requires exactly 2 nights");
        }

        if (!DateUtil.isFriday(checkIn)) {
            throw new InvalidRequestException("Weekend package must start on Friday");
        }

        if (!DateUtil.isSunday(checkOut)) {
            throw new InvalidRequestException("Weekend package must end on Sunday");
        }
    }

    @Override
    public BigDecimal calculateSubtotal(LocalDate checkIn, LocalDate checkOut, RoomType roomType) {
        return MoneyUtil.round(PACKAGE_PRICES.get(roomType));
    }

    @Override
    public BigDecimal calculateDiscount(BigDecimal subtotal) {
        return BigDecimal.ZERO;
    }

    @Override
    public BigDecimal calculateRefund(BigDecimal amountPaid) {
        return MoneyUtil.round(amountPaid.divide(new BigDecimal("2"), 10, java.math.RoundingMode.HALF_EVEN));
    }
}
