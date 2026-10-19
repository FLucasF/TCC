package com.pousadamarealta.service;

import com.pousadamarealta.exception.InvalidRequestException;
import com.pousadamarealta.model.Room;
import com.pousadamarealta.model.RoomType;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Map;

public class WeekendPackageRatePlan implements RatePlanStrategy {

    private static final Map<RoomType, BigDecimal> PACKAGE_PRICES = Map.of(
        RoomType.STANDARD, new BigDecimal("520.00"),
        RoomType.DELUXE, new BigDecimal("820.00"),
        RoomType.SUITE, new BigDecimal("1200.00")
    );

    @Override
    public void validate(LocalDate checkIn, LocalDate checkOut, int nights, String companyCode) {
        if (nights != 2) {
            throw new InvalidRequestException("O pacote de fim de semana exige exatamente 2 noites");
        }

        if (checkIn.getDayOfWeek() != DayOfWeek.FRIDAY) {
            throw new InvalidRequestException("O pacote de fim de semana deve começar na sexta-feira");
        }

        if (checkOut.getDayOfWeek() != DayOfWeek.SUNDAY) {
            throw new InvalidRequestException("O pacote de fim de semana deve terminar no domingo");
        }
    }

    @Override
    public PricingResult calculatePrice(Room room, LocalDate checkIn, LocalDate checkOut, int nights) {
        BigDecimal packagePrice = PACKAGE_PRICES.get(room.getType());
        return new PricingResult(packagePrice, BigDecimal.ZERO, packagePrice);
    }

    @Override
    public BigDecimal calculateRefund(BigDecimal amountPaid) {
        return round(amountPaid.multiply(new BigDecimal("0.50")));
    }
}
