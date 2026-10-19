package com.pousadamareal.domain;

import java.util.Optional;

public interface RatePlan {
    String getName();

    ValidationResult validate(ReservationRequest request, Room room, int nightCount);

    PricingResult calculatePrice(ReservationRequest request, Room room, int nightCount);

    RefundResult calculateRefund(long amountPaidCents);

    record ValidationResult(boolean isValid, Optional<String> errorCode, Optional<String> message) {
        public static ValidationResult valid() {
            return new ValidationResult(true, Optional.empty(), Optional.empty());
        }

        public static ValidationResult invalid(String errorCode, String message) {
            return new ValidationResult(false, Optional.of(errorCode), Optional.of(message));
        }
    }

    record PricingResult(long subtotalCents, long discountCents, long totalCents) {}

    record RefundResult(long amountCents) {}
}
