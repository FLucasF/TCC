package marealta;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.util.Map;

public class WeekendPackagePlan implements RatePlan {
    private static final Map<RoomType, BigDecimal> PRICE = Map.of(
            RoomType.STANDARD, Money.of("520.00"),
            RoomType.DELUXE, Money.of("820.00"),
            RoomType.SUITE, Money.of("1200.00"));

    public String code() { return "WEEKEND_PACKAGE"; }

    @Override
    public void validate(Stay stay) {
        boolean fridayToSunday = stay.checkIn().getDayOfWeek() == DayOfWeek.FRIDAY
                && stay.checkOut().equals(stay.checkIn().plusDays(2));
        if (!fridayToSunday) {
            throw ApiException.invalid("WEEKEND_PACKAGE só vale de sexta a domingo, com duas noites");
        }
    }

    @Override
    public Quote quote(Stay stay, BigDecimal nightsTotal) {
        return new Quote(PRICE.get(stay.room().type()), Money.ZERO);
    }

    public BigDecimal refund(BigDecimal amountPaid) { return Money.percent(amountPaid, 50); }
}
