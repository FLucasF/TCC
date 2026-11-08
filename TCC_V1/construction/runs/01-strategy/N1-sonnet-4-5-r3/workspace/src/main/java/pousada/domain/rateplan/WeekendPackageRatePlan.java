package pousada.domain.rateplan;

import pousada.domain.RoomType;

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
    public String name() {
        return "WEEKEND_PACKAGE";
    }

    @Override
    public void validate(LocalDate checkIn, LocalDate checkOut, RoomType roomType, String companyCode) {
        if (!isWeekendPackagePeriod(checkIn, checkOut)) {
            throw new IllegalArgumentException("Weekend package requires check-in on Friday and check-out on Sunday (exactly 2 nights)");
        }
    }

    @Override
    public PricingResult calculatePricing(LocalDate checkIn, LocalDate checkOut, RoomType roomType, String companyCode) {
        BigDecimal packagePrice = PACKAGE_PRICES.get(roomType);
        return new PricingResult(packagePrice, BigDecimal.ZERO, packagePrice);
    }

    @Override
    public BigDecimal calculateRefund(BigDecimal amountPaid) {
        return round(amountPaid.divide(new BigDecimal("2"), 10, java.math.RoundingMode.HALF_EVEN));
    }
}
