package br.com.marealta.plan;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.util.Map;

import br.com.marealta.domain.DomainException;
import br.com.marealta.domain.Money;
import br.com.marealta.domain.Quote;
import br.com.marealta.domain.RoomType;
import br.com.marealta.domain.Stay;

/** Sexta a domingo (2 noites) por preço fechado o ano todo; cancelamento devolve metade. */
public class WeekendPackagePlan implements RatePlan {
    private static final Map<RoomType, BigDecimal> PRICE = Map.of(
            RoomType.STANDARD, Money.of("520.00"),
            RoomType.DELUXE, Money.of("820.00"),
            RoomType.SUITE, Money.of("1200.00"));

    @Override
    public String code() {
        return "WEEKEND_PACKAGE";
    }

    @Override
    public void validate(Stay stay) {
        if (stay.checkIn().getDayOfWeek() != DayOfWeek.FRIDAY || stay.nights() != 2) {
            throw DomainException.invalid("WEEKEND_PACKAGE exige entrada na sexta e saída no domingo seguinte");
        }
    }

    @Override
    public Quote quote(Stay stay) {
        return new Quote(PRICE.get(stay.room().type()), Money.ZERO);
    }

    @Override
    public BigDecimal refund(BigDecimal paid) {
        return Money.percent(paid, "0.5");
    }
}
