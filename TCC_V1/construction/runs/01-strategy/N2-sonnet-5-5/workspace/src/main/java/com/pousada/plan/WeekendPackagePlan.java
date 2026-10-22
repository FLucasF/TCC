package com.pousada.plan;

import com.pousada.domain.Money;
import com.pousada.domain.RoomType;
import com.pousada.error.ApiException;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.util.Map;

public class WeekendPackagePlan implements RatePlan {

    private static final Map<RoomType, BigDecimal> PRICE_BY_TYPE = Map.of(
            RoomType.STANDARD, Money.of("520.00"),
            RoomType.DELUXE, Money.of("820.00"),
            RoomType.SUITE, Money.of("1200.00"));

    @Override
    public String code() {
        return "WEEKEND_PACKAGE";
    }

    @Override
    public void validate(PricingRequest request) {
        boolean fridayToSunday = request.checkIn().getDayOfWeek() == DayOfWeek.FRIDAY
                && request.nights() == 2;
        if (!fridayToSunday) {
            throw ApiException.invalidRequest("O pacote de fim de semana é de sexta a domingo, com duas noites");
        }
    }

    @Override
    public BigDecimal subtotal(PricingRequest request) {
        return PRICE_BY_TYPE.get(request.room().type());
    }

    @Override
    public BigDecimal refund(BigDecimal amountPaid) {
        return Money.percent(amountPaid, "0.50");
    }
}
