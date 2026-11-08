package br.com.maralta.domain.rateplan;

import br.com.maralta.domain.RoomType;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Map;

public class WeekendPackageRatePlan implements RatePlan {

    private static final Map<RoomType, BigDecimal> PACKAGE_PRICES = Map.of(
        RoomType.STANDARD, new BigDecimal("520.00"),
        RoomType.DELUXE, new BigDecimal("820.00"),
        RoomType.SUITE, new BigDecimal("1200.00")
    );

    @Override
    public void validate(LocalDate checkIn, LocalDate checkOut, int nights, RoomType roomType, String companyCode) {
        if (nights != 2) {
            throw new IllegalArgumentException("Weekend package requires exactly 2 nights");
        }

        if (!isFriday(checkIn)) {
            throw new IllegalArgumentException("Weekend package must start on Friday");
        }

        if (!isSunday(checkOut)) {
            throw new IllegalArgumentException("Weekend package must end on Sunday");
        }
    }

    @Override
    public BigDecimal calculateSubtotal(LocalDate checkIn, LocalDate checkOut, int nights, RoomType roomType) {
        return PACKAGE_PRICES.get(roomType);
    }

    @Override
    public BigDecimal calculateDiscount(BigDecimal subtotal) {
        return BigDecimal.ZERO;
    }

    @Override
    public BigDecimal calculateRefund(BigDecimal amountPaid) {
        BigDecimal refund = amountPaid.divide(new BigDecimal("2"), 2, RoundingMode.HALF_EVEN);
        return refund;
    }
}
