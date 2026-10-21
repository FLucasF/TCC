package pousada.plan;

import pousada.Money;
import pousada.PousadaException;
import pousada.Quote;
import pousada.RoomType;
import pousada.Stay;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.util.Map;

public class WeekendPackagePlan implements RatePlan {
    private static final Map<RoomType, BigDecimal> PRICE = Map.of(
            RoomType.STANDARD, Money.of("520.00"),
            RoomType.DELUXE, Money.of("820.00"),
            RoomType.SUITE, Money.of("1200.00"));

    public String code() { return "WEEKEND_PACKAGE"; }

    @Override
    public void validate(Stay stay) {
        if (stay.checkIn().getDayOfWeek() != DayOfWeek.FRIDAY || stay.nights() != 2) {
            throw PousadaException.invalid("O WEEKEND_PACKAGE exige entrada na sexta e saída no domingo seguinte");
        }
    }

    @Override
    public Quote quote(Stay stay) {
        return new Quote(stay.nights(), PRICE.get(stay.room().type()), Money.ZERO);
    }

    public BigDecimal refund(BigDecimal amountPaid) { return Money.fraction(amountPaid, "0.5"); }
}
