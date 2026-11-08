package br.com.pousadamarealta.domain.rateplan;

import br.com.pousadamarealta.domain.exception.InvalidRequestException;
import br.com.pousadamarealta.domain.model.PriceCalculation;
import br.com.pousadamarealta.domain.model.Reservation;
import br.com.pousadamarealta.domain.model.Room;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Map;

public class CorporateRatePlan implements RatePlan {
    private static final Map<String, BigDecimal> COMPANY_DISCOUNTS = Map.of(
        "TECNOSUL", new BigDecimal("0.12"),
        "AGROVALE", new BigDecimal("0.18"),
        "FARMAVIDA", new BigDecimal("0.25")
    );

    @Override
    public void validate(ReservationRequest request, Room room) {
        if (request.companyCode() == null || request.companyCode().trim().isEmpty()) {
            throw new InvalidRequestException("Código da empresa é obrigatório para plano corporativo");
        }

        if (!COMPANY_DISCOUNTS.containsKey(request.companyCode())) {
            throw new InvalidRequestException("Código da empresa inválido");
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
        BigDecimal discountRate = COMPANY_DISCOUNTS.get(request.companyCode());
        BigDecimal discount = subtotal.multiply(discountRate).setScale(2, RoundingMode.HALF_EVEN);
        BigDecimal total = subtotal.subtract(discount).setScale(2, RoundingMode.HALF_EVEN);

        return new PriceCalculation(subtotal, discount, total);
    }

    @Override
    public BigDecimal calculateRefund(Reservation reservation) {
        return reservation.getAmountPaid();
    }
}
