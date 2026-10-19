package br.com.mareal.plans;

import br.com.mareal.domain.ApiException;
import br.com.mareal.domain.Money;
import br.com.mareal.domain.RoomType;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.util.Map;

public class WeekendPackagePlan implements RatePlan {
    private static final Map<RoomType, BigDecimal> PRICE_BY_TYPE = Map.of(
            RoomType.STANDARD, Money.of("520.00"),
            RoomType.DELUXE, Money.of("820.00"),
            RoomType.SUITE, Money.of("1200.00"));
    private static final BigDecimal REFUND_SHARE = new BigDecimal("0.5");

    public String code() {
        return "WEEKEND_PACKAGE";
    }

    public Pricing price(Stay stay) {
        boolean fridayToSunday = stay.checkIn().getDayOfWeek() == DayOfWeek.FRIDAY && stay.nights() == 2;
        if (!fridayToSunday) {
            throw ApiException.invalid("O WEEKEND_PACKAGE só vale com entrada na sexta e saída no domingo seguinte.");
        }
        return Pricing.noDiscount(PRICE_BY_TYPE.get(stay.roomType()));
    }

    public BigDecimal refund(BigDecimal amountPaid) {
        return Money.percent(amountPaid, REFUND_SHARE);
    }
}
