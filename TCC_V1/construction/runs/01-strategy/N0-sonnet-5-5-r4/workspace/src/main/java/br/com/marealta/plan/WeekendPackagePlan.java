package br.com.marealta.plan;

import br.com.marealta.ApiException;
import br.com.marealta.Money;
import br.com.marealta.RoomType;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.util.Map;

/** Pacote de fim de semana: sexta a domingo (2 noites) por preço fechado o ano todo. */
public final class WeekendPackagePlan implements RatePlan {
    private static final Map<RoomType, BigDecimal> PRICES = Map.of(
            RoomType.STANDARD, Money.of("520.00"),
            RoomType.DELUXE, Money.of("820.00"),
            RoomType.SUITE, Money.of("1200.00"));
    private static final BigDecimal HALF = new BigDecimal("0.5");

    @Override public String code() { return "WEEKEND_PACKAGE"; }

    @Override public Quote quote(PricingContext ctx) {
        if (ctx.checkIn().getDayOfWeek() != DayOfWeek.FRIDAY || ctx.nights() != 2) {
            throw ApiException.invalid("O WEEKEND_PACKAGE exige entrada na sexta e saída no domingo seguinte.");
        }
        return Quote.withoutDiscount(PRICES.get(ctx.room().type()));
    }

    @Override public BigDecimal refund(BigDecimal amountPaid) { return Money.times(amountPaid, HALF); }
}
