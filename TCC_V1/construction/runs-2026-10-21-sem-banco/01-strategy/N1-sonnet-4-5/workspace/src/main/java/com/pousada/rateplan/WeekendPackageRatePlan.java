package com.pousada.rateplan;

import com.pousada.exception.InvalidRequestException;
import com.pousada.model.Room;
import com.pousada.model.RoomType;
import com.pousada.model.RatePlanType;
import com.pousada.util.DateUtil;
import com.pousada.util.MoneyUtil;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Map;

public class WeekendPackageRatePlan implements RatePlan {

    private static final Map<RoomType, BigDecimal> PACKAGE_PRICES = Map.of(
        RoomType.STANDARD, new BigDecimal("520.00"),
        RoomType.DELUXE, new BigDecimal("820.00"),
        RoomType.SUITE, new BigDecimal("1200.00")
    );

    @Override
    public RatePlanType getType() {
        return RatePlanType.WEEKEND_PACKAGE;
    }

    @Override
    public void validate(Room room, LocalDate checkIn, LocalDate checkOut, int guests, String companyCode) {
        long nights = ChronoUnit.DAYS.between(checkIn, checkOut);

        if (nights != 2) {
            throw new InvalidRequestException("Pacote de fim de semana requer exatamente 2 noites");
        }

        if (!DateUtil.isFriday(checkIn)) {
            throw new InvalidRequestException("Pacote de fim de semana requer entrada na sexta-feira");
        }

        if (!DateUtil.isSunday(checkOut)) {
            throw new InvalidRequestException("Pacote de fim de semana requer saída no domingo");
        }
    }

    @Override
    public BigDecimal calculateSubtotal(Room room, LocalDate checkIn, LocalDate checkOut) {
        return PACKAGE_PRICES.get(room.getType());
    }

    @Override
    public BigDecimal calculateDiscount(BigDecimal subtotal) {
        return MoneyUtil.round(BigDecimal.ZERO);
    }

    @Override
    public BigDecimal calculateRefund(BigDecimal amountPaid) {
        return MoneyUtil.round(amountPaid.divide(new BigDecimal("2")));
    }
}
