package br.com.marealta.plan;

import br.com.marealta.domain.ApiException;
import br.com.marealta.domain.Money;
import br.com.marealta.domain.Pricing;
import br.com.marealta.domain.RoomType;
import br.com.marealta.domain.Stay;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.util.Map;

/** Sexta a domingo (duas noites) por preço fechado o ano todo; o cancelamento devolve metade do pago. */
class WeekendPackagePlan implements RatePlan {

    private static final Map<RoomType, BigDecimal> PRICE_BY_TYPE = Map.of(
            RoomType.STANDARD, Money.of("520.00"),
            RoomType.DELUXE, Money.of("820.00"),
            RoomType.SUITE, Money.of("1200.00"));

    @Override
    public String code() {
        return "WEEKEND_PACKAGE";
    }

    @Override
    public Pricing quote(Stay stay) {
        boolean fridayToSunday = stay.checkIn().getDayOfWeek() == DayOfWeek.FRIDAY && stay.nights() == 2;
        if (!fridayToSunday) {
            throw ApiException.invalidRequest("O plano WEEKEND_PACKAGE exige entrada na sexta e saída no domingo");
        }
        return new Pricing(PRICE_BY_TYPE.get(stay.room().type()), Money.ZERO);
    }

    @Override
    public BigDecimal refund(BigDecimal amountPaid) {
        return Money.round(amountPaid.divide(new BigDecimal("2"), 10, RoundingMode.HALF_EVEN));
    }
}
