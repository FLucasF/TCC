package br.com.marealta.plan;

import br.com.marealta.DomainException;
import br.com.marealta.domain.Money;
import br.com.marealta.domain.RoomType;
import br.com.marealta.domain.Stay;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.util.Map;

/** Preço fechado para sexta a domingo, o ano inteiro. */
public class WeekendPackagePlan implements RatePlan {
    private static final Map<RoomType, BigDecimal> PRICES = Map.of(
            RoomType.STANDARD, Money.of("520.00"),
            RoomType.DELUXE, Money.of("820.00"),
            RoomType.SUITE, Money.of("1200.00"));

    @Override public String code() { return "WEEKEND_PACKAGE"; }

    @Override
    public Quote quote(Stay stay) {
        if (stay.checkIn().getDayOfWeek() != DayOfWeek.FRIDAY || stay.nights() != 2) {
            throw DomainException.invalid("O WEEKEND_PACKAGE exige entrada na sexta e saída no domingo seguinte");
        }
        BigDecimal price = PRICES.get(stay.room().type());
        return new Quote(2, price, Money.ZERO, price);
    }

    @Override
    public BigDecimal refund(BigDecimal amountPaid) {
        return Money.round(amountPaid.multiply(new BigDecimal("0.5")));
    }
}
