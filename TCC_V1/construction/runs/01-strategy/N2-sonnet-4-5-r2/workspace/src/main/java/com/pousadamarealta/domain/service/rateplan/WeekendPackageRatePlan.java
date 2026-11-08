package com.pousadamarealta.domain.service.rateplan;

import com.pousadamarealta.domain.model.RoomType;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Map;

import static com.pousadamarealta.domain.service.rateplan.SeasonCalculator.roundHalfToEven;

public class WeekendPackageRatePlan implements RatePlan {
    private static final Map<RoomType, BigDecimal> PACKAGE_PRICES = Map.of(
        RoomType.STANDARD, new BigDecimal("520.00"),
        RoomType.DELUXE, new BigDecimal("820.00"),
        RoomType.SUITE, new BigDecimal("1200.00")
    );

    @Override
    public void validate(LocalDate checkIn, LocalDate checkOut, String companyCode) {
        long nights = ChronoUnit.DAYS.between(checkIn, checkOut);
        if (nights != 2) {
            throw new IllegalArgumentException("Pacote de fim de semana requer exatamente 2 noites");
        }
        if (checkIn.getDayOfWeek() != DayOfWeek.FRIDAY) {
            throw new IllegalArgumentException("Pacote de fim de semana deve começar na sexta-feira");
        }
        if (checkOut.getDayOfWeek() != DayOfWeek.SUNDAY) {
            throw new IllegalArgumentException("Pacote de fim de semana deve terminar no domingo");
        }
    }

    @Override
    public BigDecimal calculateSubtotal(RoomType roomType, LocalDate checkIn, LocalDate checkOut) {
        return PACKAGE_PRICES.get(roomType);
    }

    @Override
    public BigDecimal calculateDiscount(BigDecimal subtotal, RoomType roomType, LocalDate checkIn, LocalDate checkOut) {
        return BigDecimal.ZERO;
    }

    @Override
    public BigDecimal calculateRefund(BigDecimal amountPaid) {
        return roundHalfToEven(amountPaid.multiply(new BigDecimal("0.5")));
    }
}
