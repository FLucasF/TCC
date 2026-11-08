package pousada.plan;

import pousada.ApiException;
import pousada.Money;
import pousada.RoomType;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.util.Map;

/** Sexta a domingo, duas noites, preço fechado o ano todo. Cancelando, volta metade do pago. */
public class WeekendPackagePlan implements RatePlan {
    private static final Map<RoomType, BigDecimal> PRICES = Map.of(
            RoomType.STANDARD, new BigDecimal("520.00"),
            RoomType.DELUXE, new BigDecimal("820.00"),
            RoomType.SUITE, new BigDecimal("1200.00"));
    private static final BigDecimal HALF = new BigDecimal("0.5");

    public String code() { return "WEEKEND_PACKAGE"; }

    @Override
    public void validate(BookingContext ctx) {
        if (ctx.checkIn().getDayOfWeek() != DayOfWeek.FRIDAY || ctx.nights() != 2) {
            throw ApiException.invalid("O WEEKEND_PACKAGE exige entrada na sexta e saída no domingo seguinte (2 noites)");
        }
    }

    public Quote quote(BookingContext ctx) {
        return new Quote(PRICES.get(ctx.room().type()), BigDecimal.ZERO);
    }

    public BigDecimal refund(BigDecimal amountPaid) { return Money.times(amountPaid, HALF); }
}
