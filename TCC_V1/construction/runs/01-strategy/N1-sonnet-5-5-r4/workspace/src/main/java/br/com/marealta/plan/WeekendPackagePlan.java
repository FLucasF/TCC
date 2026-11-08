package br.com.marealta.plan;

import br.com.marealta.ApiError;
import br.com.marealta.Money;
import br.com.marealta.RoomType;
import br.com.marealta.Stay;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.util.Map;

/** Sexta a domingo, duas noites, preço fechado o ano todo; cancelando, volta metade. */
public class WeekendPackagePlan extends RatePlan {
    private static final Map<RoomType, BigDecimal> PRICE = Map.of(
            RoomType.STANDARD, Money.of("520.00"),
            RoomType.DELUXE, Money.of("820.00"),
            RoomType.SUITE, Money.of("1200.00"));
    private static final BigDecimal REFUND = new BigDecimal("0.5");

    public WeekendPackagePlan() {
        super("WEEKEND_PACKAGE");
    }

    @Override
    protected void validate(Stay stay) {
        if (stay.checkIn().getDayOfWeek() != DayOfWeek.FRIDAY || stay.nights() != 2) {
            throw ApiError.invalidRequest("O pacote de fim de semana é de sexta a domingo, duas noites");
        }
    }

    @Override
    protected BigDecimal subtotal(Stay stay) {
        return PRICE.get(stay.room().type());
    }

    @Override
    public BigDecimal refund(BigDecimal paid) {
        return Money.fraction(paid, REFUND);
    }
}
