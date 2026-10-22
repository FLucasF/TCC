package br.com.marealta.plan;

import br.com.marealta.ApiException;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.util.Map;

public class WeekendPackagePlan extends AbstractRatePlan {
    private static final Map<String, BigDecimal> PRICES = Map.of(
            "STANDARD", Money.of("520.00"),
            "DELUXE", Money.of("820.00"),
            "SUITE", Money.of("1200.00"));

    public String code() { return "WEEKEND_PACKAGE"; }

    public Quote quote(QuoteRequest r) {
        if (r.checkIn().getDayOfWeek() != DayOfWeek.FRIDAY || !r.checkOut().equals(r.checkIn().plusDays(2))) {
            throw ApiException.invalid("O WEEKEND_PACKAGE só vale com entrada na sexta e saída no domingo seguinte");
        }
        BigDecimal price = PRICES.get(r.room().type());
        if (price == null) {
            throw ApiException.invalid("Não há pacote de fim de semana para o tipo " + r.room().type());
        }
        return new Quote(2, price, Money.round(BigDecimal.ZERO));
    }

    public BigDecimal refund(BigDecimal paid) { return Money.percentOf(paid, "0.5"); }
}
