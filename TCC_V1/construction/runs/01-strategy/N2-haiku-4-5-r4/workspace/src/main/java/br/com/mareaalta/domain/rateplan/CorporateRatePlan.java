package br.com.mareaalta.domain.rateplan;

import br.com.mareaalta.domain.Room;
import br.com.mareaalta.util.DateUtils;
import br.com.mareaalta.util.Money;
import java.time.LocalDate;
import java.util.Map;

public class CorporateRatePlan implements RatePlan {
    private static final Map<String, Double> COMPANY_DISCOUNTS = Map.of(
            "TECNOSUL", 0.12,
            "AGROVALE", 0.18,
            "FARMAVIDA", 0.25
    );

    @Override
    public ValidationError validate(LocalDate checkIn, LocalDate checkOut, int guests, Room room, String companyCode) {
        if (companyCode == null || companyCode.isBlank()) {
            return ValidationError.invalid("INVALID_REQUEST", "Código de empresa é obrigatório");
        }

        if (!COMPANY_DISCOUNTS.containsKey(companyCode.toUpperCase())) {
            return ValidationError.invalid("INVALID_REQUEST", "Código de empresa inválido");
        }

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

        return new PricingResult(subtotal, 0.0, subtotal);
    }

    @Override
    public double getRefundPercentage() {
        return 100.0;
    }

    public PricingResult calculatePriceWithCompany(LocalDate checkIn, LocalDate checkOut, Room room, String companyCode) {
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

        double discountPercentage = COMPANY_DISCOUNTS.getOrDefault(companyCode.toUpperCase(), 0.0);
        double discount = Money.round(subtotal * discountPercentage);
        double total = Money.round(subtotal - discount);

        return new PricingResult(subtotal, discount, total);
    }

    public double getCompanyDiscount(String companyCode) {
        return COMPANY_DISCOUNTS.getOrDefault(companyCode.toUpperCase(), 0.0);
    }
}
