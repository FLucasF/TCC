package com.marealta.plan;

import com.marealta.ApiException;
import com.marealta.Money;
import com.marealta.RoomType;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.util.Map;

/** Sexta a domingo, duas noites, preço fechado o ano todo. */
class WeekendPackagePlan implements RatePlan {
    private static final Map<RoomType, BigDecimal> PRICES = Map.of(
            RoomType.STANDARD, Money.of("520.00"),
            RoomType.DELUXE, Money.of("820.00"),
            RoomType.SUITE, Money.of("1200.00"));

    @Override public String code() { return "WEEKEND_PACKAGE"; }

    @Override public Quote quote(BookingTerms t) {
        if (t.checkIn().getDayOfWeek() != DayOfWeek.FRIDAY || !t.checkOut().equals(t.checkIn().plusDays(2))) {
            throw ApiException.invalid("O WEEKEND_PACKAGE exige entrada na sexta e saída no domingo seguinte");
        }
        BigDecimal price = PRICES.get(t.room().type());
        return new Quote(2, price, Money.ZERO, price);
    }

    @Override public BigDecimal refund(BigDecimal amountPaid) {
        return Money.percent(amountPaid, "0.5");
    }
}
