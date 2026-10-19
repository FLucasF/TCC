package com.pousada.domain.rateplan;

import com.pousada.domain.PriceCalculation;
import com.pousada.domain.Room;
import com.pousada.domain.RoomType;
import com.pousada.domain.ValidationResult;
import com.pousada.util.DateUtils;
import com.pousada.util.MoneyUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

public class WeekendPackageRatePlan implements RatePlan {

    private static final Map<RoomType, BigDecimal> PACKAGE_PRICES = Map.of(
        RoomType.STANDARD, new BigDecimal("520.00"),
        RoomType.DELUXE, new BigDecimal("820.00"),
        RoomType.SUITE, new BigDecimal("1200.00")
    );

    @Override
    public ValidationResult validate(LocalDate checkIn, LocalDate checkOut, int nights, String companyCode) {
        if (nights != 2) {
            return ValidationResult.invalid("Weekend package requires exactly 2 nights");
        }

        if (!DateUtils.isFriday(checkIn)) {
            return ValidationResult.invalid("Weekend package requires Friday check-in");
        }

        if (!DateUtils.isSunday(checkOut)) {
            return ValidationResult.invalid("Weekend package requires Sunday check-out");
        }

        return ValidationResult.valid();
    }

    @Override
    public PriceCalculation calculatePrice(Room room, LocalDate checkIn, LocalDate checkOut, int nights) {
        BigDecimal packagePrice = PACKAGE_PRICES.get(room.getType());
        BigDecimal roundedPrice = MoneyUtils.roundMoney(packagePrice);
        BigDecimal discount = MoneyUtils.roundMoney(BigDecimal.ZERO);

        return new PriceCalculation(roundedPrice, discount, roundedPrice);
    }

    @Override
    public BigDecimal calculateRefund(BigDecimal amountPaid) {
        return MoneyUtils.roundMoney(amountPaid.divide(BigDecimal.valueOf(2)));
    }
}
