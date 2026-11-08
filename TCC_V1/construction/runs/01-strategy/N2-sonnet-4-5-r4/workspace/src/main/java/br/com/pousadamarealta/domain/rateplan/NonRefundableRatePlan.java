package br.com.pousadamarealta.domain.rateplan;

import br.com.pousadamarealta.domain.model.PriceCalculation;
import br.com.pousadamarealta.domain.model.Reservation;
import br.com.pousadamarealta.domain.model.Room;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

public class NonRefundableRatePlan implements RatePlan {
    private static final BigDecimal DISCOUNT_RATE = new BigDecimal("0.15");

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
        BigDecimal discount = subtotal.multiply(DISCOUNT_RATE).setScale(2, RoundingMode.HALF_EVEN);
        BigDecimal total = subtotal.subtract(discount).setScale(2, RoundingMode.HALF_EVEN);

        return new PriceCalculation(subtotal, discount, total);
    }

    @Override
    public BigDecimal calculateRefund(Reservation reservation) {
        return BigDecimal.ZERO;
    }
}
