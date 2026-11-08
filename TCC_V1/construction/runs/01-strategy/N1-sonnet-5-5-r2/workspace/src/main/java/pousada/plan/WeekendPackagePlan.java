package pousada.plan;

import pousada.domain.AppException;
import pousada.domain.ErrorCode;
import pousada.domain.Money;
import pousada.domain.RoomType;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.util.Map;

public class WeekendPackagePlan implements RatePlan {
    private static final Map<RoomType, BigDecimal> PACKAGE_PRICE = Map.of(
            RoomType.STANDARD, Money.of("520.00"),
            RoomType.DELUXE, Money.of("820.00"),
            RoomType.SUITE, Money.of("1200.00"));

    @Override
    public String code() {
        return "WEEKEND_PACKAGE";
    }

    @Override
    public void validate(Stay stay) {
        if (stay.checkIn().getDayOfWeek() != DayOfWeek.FRIDAY || stay.nights() != 2) {
            throw new AppException(ErrorCode.INVALID_REQUEST,
                    "O pacote de fim de semana é de sexta a domingo, exatamente duas noites");
        }
    }

    @Override
    public BigDecimal subtotal(Stay stay) {
        return PACKAGE_PRICE.get(stay.room().type());
    }

    @Override
    public BigDecimal discount(Stay stay, BigDecimal subtotal) {
        return Money.ZERO;
    }

    @Override
    public BigDecimal refund(BigDecimal amountPaid) {
        return Money.fraction(amountPaid, "0.5");
    }
}
