package br.com.marealta.plan;

import br.com.marealta.domain.Money;
import br.com.marealta.domain.Quote;
import br.com.marealta.domain.RoomType;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.util.Map;

/** Pacote de fim de semana: sexta a domingo, preço fechado o ano todo. */
public class WeekendPackagePlan implements RatePlan {
    private static final Map<RoomType, BigDecimal> PRICE = Map.of(
            RoomType.STANDARD, new BigDecimal("520.00"),
            RoomType.DELUXE, new BigDecimal("820.00"),
            RoomType.SUITE, new BigDecimal("1200.00"));

    public String code() { return "WEEKEND_PACKAGE"; }

    public Quote quote(BookingRequest r) {
        if (r.checkIn().getDayOfWeek() != DayOfWeek.FRIDAY || r.nights() != 2) {
            throw new PlanRuleException("O pacote de fim de semana exige entrada na sexta e saída no domingo seguinte");
        }
        return Quote.of(2, Money.round(PRICE.get(r.room().type())), Money.ZERO);
    }

    public BigDecimal refund(BigDecimal amountPaid) { return Money.percent(amountPaid, 50); }
}
