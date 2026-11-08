package br.com.pousadamarealta.domain.rateplan;

import br.com.pousadamarealta.domain.exception.InvalidRequestException;
import br.com.pousadamarealta.domain.model.PriceCalculation;
import br.com.pousadamarealta.domain.model.Reservation;
import br.com.pousadamarealta.domain.model.Room;
import br.com.pousadamarealta.domain.model.RoomType;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.temporal.ChronoUnit;
import java.util.Map;

public class WeekendPackageRatePlan implements RatePlan {
    private static final Map<RoomType, BigDecimal> PACKAGE_PRICES = Map.of(
        RoomType.STANDARD, new BigDecimal("520.00"),
        RoomType.DELUXE, new BigDecimal("820.00"),
        RoomType.SUITE, new BigDecimal("1200.00")
    );

    @Override
    public void validate(ReservationRequest request, Room room) {
        long nights = ChronoUnit.DAYS.between(request.checkIn(), request.checkOut());

        if (nights != 2) {
            throw new InvalidRequestException("Pacote de fim de semana requer exatamente 2 noites");
        }

        if (!SeasonCalculator.isFriday(request.checkIn())) {
            throw new InvalidRequestException("Pacote de fim de semana requer entrada na sexta-feira");
        }

        if (!SeasonCalculator.isSunday(request.checkOut())) {
            throw new InvalidRequestException("Pacote de fim de semana requer saída no domingo");
        }
    }

    @Override
    public PriceCalculation calculatePrice(ReservationRequest request, Room room) {
        BigDecimal packagePrice = PACKAGE_PRICES.get(room.getType());
        BigDecimal discount = BigDecimal.ZERO;

        return new PriceCalculation(packagePrice, discount, packagePrice);
    }

    @Override
    public BigDecimal calculateRefund(Reservation reservation) {
        return reservation.getAmountPaid()
            .multiply(new BigDecimal("0.50"))
            .setScale(2, RoundingMode.HALF_EVEN);
    }
}
