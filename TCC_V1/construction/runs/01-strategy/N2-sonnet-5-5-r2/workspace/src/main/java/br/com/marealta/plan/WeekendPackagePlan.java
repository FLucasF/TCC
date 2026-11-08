package br.com.marealta.plan;

import br.com.marealta.domain.BusinessException;
import br.com.marealta.domain.Money;
import br.com.marealta.domain.Quote;
import br.com.marealta.domain.RoomType;
import br.com.marealta.domain.Stay;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.util.Map;

/** Sexta a domingo, duas noites, por preço fechado o ano todo. */
final class WeekendPackagePlan implements RatePlan {
    private static final Map<RoomType, BigDecimal> PRICE = Map.of(
            RoomType.STANDARD, Money.of("520.00"),
            RoomType.DELUXE, Money.of("820.00"),
            RoomType.SUITE, Money.of("1200.00"));
    private static final BigDecimal REFUND_RATE = new BigDecimal("0.50");

    @Override
    public String code() {
        return "WEEKEND_PACKAGE";
    }

    @Override
    public Quote quote(Stay stay) {
        boolean friday = stay.checkIn().getDayOfWeek() == DayOfWeek.FRIDAY;
        if (!friday || stay.nights() != 2) {
            throw BusinessException.invalid("O WEEKEND_PACKAGE exige entrada na sexta e saída no domingo seguinte");
        }
        return new Quote(2, PRICE.get(stay.room().type()), Money.ZERO);
    }

    @Override
    public BigDecimal refund(BigDecimal amountPaid) {
        return Money.fraction(amountPaid, REFUND_RATE);
    }
}
