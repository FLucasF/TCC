package br.com.pousadamarealta.domain.rateplan;

import br.com.pousadamarealta.domain.model.PriceCalculation;
import br.com.pousadamarealta.domain.model.Reservation;
import br.com.pousadamarealta.domain.model.Room;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

public class StandardRatePlan implements RatePlan {

    @Override
    public void validate(ReservationRequest request, Room room) {
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
        BigDecimal discount = BigDecimal.ZERO;
        BigDecimal total = subtotal;

        return new PriceCalculation(subtotal, discount, total);
    }

    @Override
    public BigDecimal calculateRefund(Reservation reservation) {
        return reservation.getAmountPaid();
    }
}
