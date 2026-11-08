package br.com.mareaalta.domain.rateplan;

import br.com.mareaalta.domain.Room;
import br.com.mareaalta.util.DateUtils;
import br.com.mareaalta.util.Money;
import java.time.LocalDate;

public class StandardRatePlan implements RatePlan {
    @Override
    public ValidationError validate(LocalDate checkIn, LocalDate checkOut, int guests, Room room, String companyCode) {
        if (guests < 1) {
            return ValidationError.invalid("INVALID_REQUEST", "Mínimo de 1 hóspede");
        }
        if (guests > room.getCapacity()) {
            return ValidationError.invalid("CAPACITY_EXCEEDED", "Número de hóspedes excede a capacidade");
        }
        if (!checkOut.isAfter(checkIn)) {
            return ValidationError.invalid("INVALID_REQUEST", "Check-out deve ser depois do check-in");
        }
        int nights = DateUtils.nightsBetween(checkIn, checkOut);
        if (nights < 1) {
            return ValidationError.invalid("INVALID_REQUEST", "Mínimo de 1 noite");
        }
        return null;
    }

    @Override
    public PricingResult calculatePrice(LocalDate checkIn, LocalDate checkOut, Room room) {
        double nightlyRate = room.getNightlyRate();
        int nights = DateUtils.nightsBetween(checkIn, checkOut);
        double subtotal = 0.0;

        for (int i = 0; i < nights; i++) {
            LocalDate night = checkIn.plusDays(i);
            double nightPrice = nightlyRate;

            if (DateUtils.isHighSeason(night)) {
                nightPrice = Money.round(nightPrice * 1.20);
            }

            subtotal = Money.round(subtotal + nightPrice);
        }

        double discount = 0.0;
        double total = Money.round(subtotal - discount);

        return new PricingResult(subtotal, discount, total);
    }

    @Override
    public double getRefundPercentage() {
        return 100.0;
    }
}
