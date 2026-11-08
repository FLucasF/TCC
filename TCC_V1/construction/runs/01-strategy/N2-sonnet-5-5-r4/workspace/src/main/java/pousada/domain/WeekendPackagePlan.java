package pousada.domain;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.util.Map;

/** Sexta a domingo, preço fechado por tipo de quarto, o ano todo. */
public class WeekendPackagePlan implements RatePlan {
    private final Map<RoomType, BigDecimal> priceByType;

    public WeekendPackagePlan(Map<RoomType, BigDecimal> priceByType) {
        this.priceByType = Map.copyOf(priceByType);
    }

    @Override public String code() { return "WEEKEND_PACKAGE"; }

    @Override
    public void validate(Booking booking) {
        boolean fridayToSunday = booking.checkIn().getDayOfWeek() == DayOfWeek.FRIDAY
                && booking.checkOut().equals(booking.checkIn().plusDays(2));
        if (!fridayToSunday) {
            throw ApiException.invalidRequest("O WEEKEND_PACKAGE exige entrada na sexta e saída no domingo seguinte");
        }
    }

    @Override
    public Quote quote(Booking booking) {
        return new Quote(Money.round(priceByType.get(booking.room().type())), Money.ZERO);
    }

    @Override
    public BigDecimal refund(BigDecimal amountPaid) {
        return Money.portion(amountPaid, new BigDecimal("0.5"));
    }
}
