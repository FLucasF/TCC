package br.com.mare.plan;

import br.com.mare.domain.ApiException;
import br.com.mare.domain.Money;
import br.com.mare.domain.RoomType;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.util.Map;

/** Sexta a domingo, duas noites, preço fechado por tipo de quarto. */
class WeekendPackagePlan implements RatePlan {
    private static final Map<RoomType, BigDecimal> PRICES = Map.of(
            RoomType.STANDARD, Money.of("520.00"),
            RoomType.DELUXE, Money.of("820.00"),
            RoomType.SUITE, Money.of("1200.00"));
    private static final BigDecimal REFUND_RATE = new BigDecimal("0.50");

    @Override
    public String code() { return "WEEKEND_PACKAGE"; }

    @Override
    public Quote quote(Booking booking) {
        boolean friday = booking.checkIn().getDayOfWeek() == DayOfWeek.FRIDAY;
        if (!friday || booking.nights() != 2) {
            throw ApiException.invalid("O WEEKEND_PACKAGE exige entrada na sexta e saída no domingo seguinte");
        }
        BigDecimal price = PRICES.get(booking.room().type());
        return new Quote(2, price, Money.ZERO, price);
    }

    @Override
    public BigDecimal refund(BigDecimal amountPaid) {
        return Money.round(amountPaid.multiply(REFUND_RATE));
    }
}
