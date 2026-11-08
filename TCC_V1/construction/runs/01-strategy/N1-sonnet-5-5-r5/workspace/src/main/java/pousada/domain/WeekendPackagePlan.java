package pousada.domain;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.util.Map;

/** Sexta a domingo, duas noites, preço fechado o ano todo. */
class WeekendPackagePlan implements RatePlan {
    private static final Map<RoomType, BigDecimal> PRICE_BY_ROOM_TYPE = Map.of(
            RoomType.STANDARD, Money.of("520.00"),
            RoomType.DELUXE, Money.of("820.00"),
            RoomType.SUITE, Money.of("1200.00"));

    @Override
    public String code() {
        return "WEEKEND_PACKAGE";
    }

    @Override
    public void validate(Booking booking) {
        if (booking.checkIn().getDayOfWeek() != DayOfWeek.FRIDAY || booking.nights() != 2) {
            throw DomainException.invalidRequest(
                    "O WEEKEND_PACKAGE exige entrada na sexta e saída no domingo seguinte.");
        }
    }

    @Override
    public BigDecimal subtotal(Booking booking) {
        return PRICE_BY_ROOM_TYPE.get(booking.room().type());
    }

    @Override
    public BigDecimal refund(BigDecimal amountPaid) {
        return Money.percent(amountPaid, 50);
    }
}
