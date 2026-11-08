package br.com.marealta.plan;

import br.com.marealta.ApiException;
import br.com.marealta.Money;
import br.com.marealta.RoomType;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.util.Map;

/** Pacote de fim de semana: sexta a domingo, preço fechado o ano todo, reembolso de metade. */
public class WeekendPackagePlan implements RatePlan {
    private static final Map<RoomType, BigDecimal> PRICES = Map.of(
            RoomType.STANDARD, Money.of("520.00"),
            RoomType.DELUXE, Money.of("820.00"),
            RoomType.SUITE, Money.of("1200.00"));

    @Override
    public String code() {
        return "WEEKEND_PACKAGE";
    }

    @Override
    public void validate(Booking b) {
        if (b.checkIn().getDayOfWeek() != DayOfWeek.FRIDAY || b.nights() != 2) {
            throw ApiException.invalid("O WEEKEND_PACKAGE exige entrada na sexta e saída no domingo seguinte.");
        }
    }

    @Override
    public Price price(Booking b, BigDecimal nightsSum) {
        return new Price(PRICES.get(b.roomType()), Money.ZERO);
    }

    @Override
    public BigDecimal refund(BigDecimal amountPaid) {
        return Money.percent(amountPaid, 50);
    }
}
