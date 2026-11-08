package br.com.mareaalta.domain.rateplan;

import br.com.mareaalta.domain.Room;
import java.time.LocalDate;

public interface RatePlan {
    ValidationError validate(LocalDate checkIn, LocalDate checkOut, int guests, Room room, String companyCode);

    PricingResult calculatePrice(LocalDate checkIn, LocalDate checkOut, Room room);

    double getRefundPercentage();

    record ValidationError(String code, String message) {
        public static ValidationError invalid(String code, String message) {
            return new ValidationError(code, message);
        }
    }

    record PricingResult(double subtotal, double discount, double total) {}
}
