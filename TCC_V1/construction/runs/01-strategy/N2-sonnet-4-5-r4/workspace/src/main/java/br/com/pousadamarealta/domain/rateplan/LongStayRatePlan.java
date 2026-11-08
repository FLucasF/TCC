package br.com.pousadamarealta.domain.rateplan;

import br.com.pousadamarealta.domain.exception.InvalidRequestException;
import br.com.pousadamarealta.domain.model.PriceCalculation;
import br.com.pousadamarealta.domain.model.Reservation;
import br.com.pousadamarealta.domain.model.Room;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public class LongStayRatePlan implements RatePlan {
    private static final BigDecimal DISCOUNT_RATE = new BigDecimal("0.10");
    private static final int MINIMUM_NIGHTS = 7;

    @Override
    public void validate(ReservationRequest request, Room room) {
        long nights = ChronoUnit.DAYS.between(request.checkIn(), request.checkOut());

        if (nights < MINIMUM_NIGHTS) {
            throw new InvalidRequestException("Estadia longa requer no mínimo 7 noites");
        }
    }

    @Override
    public PriceCalculation calculatePrice(ReservationRequest request, Room room) {
        BigDecimal subtotal = BigDecimal.ZERO;
        LocalDate current = request.checkIn();

        while (current.isBefore(request.checkOut())) {
            BigDecimal nightRate = SeasonCalculator.applySeasonRate(
                room.getType().getNightlyRate(),
                current
            );
            subtotal = subtotal.add(nightRate);
            current = current.plusDays(1);
        }

        subtotal = subtotal.setScale(2, RoundingMode.HALF_EVEN);
        BigDecimal discount = subtotal.multiply(DISCOUNT_RATE).setScale(2, RoundingMode.HALF_EVEN);
        BigDecimal total = subtotal.subtract(discount).setScale(2, RoundingMode.HALF_EVEN);

        return new PriceCalculation(subtotal, discount, total);
    }

    @Override
    public BigDecimal calculateRefund(Reservation reservation) {
        return reservation.getAmountPaid();
    }
}
