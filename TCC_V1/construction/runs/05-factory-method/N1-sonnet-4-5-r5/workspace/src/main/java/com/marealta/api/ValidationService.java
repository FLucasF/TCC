package com.marealta.api;

import com.marealta.api.dto.CreateReservationRequest;
import com.marealta.domain.model.RatePlan;
import com.marealta.domain.model.Room;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;

public class ValidationService {

    public static class ValidationException extends RuntimeException {
        private final String errorCode;

        public ValidationException(String errorCode, String message) {
            super(message);
            this.errorCode = errorCode;
        }

        public String getErrorCode() {
            return errorCode;
        }
    }

    public void validateCreateReservation(CreateReservationRequest request) {
        if (request.roomNumber() == null || request.guestName() == null ||
            request.guestEmail() == null || request.checkIn() == null ||
            request.checkOut() == null || request.guests() == null ||
            request.ratePlan() == null) {
            throw new ValidationException("INVALID_REQUEST", "Missing required fields");
        }

        validateGuestIdentification(request);

        if (request.checkOut().isBefore(request.checkIn()) ||
            request.checkOut().equals(request.checkIn())) {
            throw new ValidationException("INVALID_REQUEST", "Check-out must be after check-in");
        }

        if (request.guests() < 1) {
            throw new ValidationException("INVALID_REQUEST", "Number of guests must be at least 1");
        }

        try {
            RatePlan.valueOf(request.ratePlan());
        } catch (IllegalArgumentException e) {
            throw new ValidationException("INVALID_REQUEST", "Invalid rate plan");
        }
    }

    private void validateGuestIdentification(CreateReservationRequest request) {
        boolean hasCpf = request.guestCpf() != null;
        boolean hasCompany = request.companyCnpj() != null && request.companyName() != null;
        boolean hasPassport = request.guestPassport() != null && request.guestCountry() != null;

        int count = (hasCpf ? 1 : 0) + (hasCompany ? 1 : 0) + (hasPassport ? 1 : 0);

        if (count != 1) {
            throw new ValidationException("INVALID_REQUEST",
                "Must provide exactly one type of guest identification: CPF, or CNPJ with company name, or passport with country");
        }

        if (request.companyCnpj() != null && request.companyName() == null) {
            throw new ValidationException("INVALID_REQUEST", "Company name is required when CNPJ is provided");
        }

        if (request.companyName() != null && request.companyCnpj() == null) {
            throw new ValidationException("INVALID_REQUEST", "CNPJ is required when company name is provided");
        }

        if (request.guestPassport() != null && request.guestCountry() == null) {
            throw new ValidationException("INVALID_REQUEST", "Country is required when passport is provided");
        }

        if (request.guestCountry() != null && request.guestPassport() == null) {
            throw new ValidationException("INVALID_REQUEST", "Passport is required when country is provided");
        }
    }

    public void validateCapacity(Room room, int guests) {
        if (guests > room.capacity()) {
            throw new ValidationException("CAPACITY_EXCEEDED",
                String.format("Room %s can accommodate up to %d guests", room.number(), room.capacity()));
        }
    }

    public LocalDate parseDate(String date) {
        try {
            return LocalDate.parse(date);
        } catch (DateTimeParseException e) {
            throw new ValidationException("INVALID_REQUEST", "Invalid date format. Use yyyy-MM-dd");
        }
    }

    public YearMonth parseMonth(String month) {
        try {
            return YearMonth.parse(month);
        } catch (DateTimeParseException e) {
            throw new ValidationException("INVALID_REQUEST", "Invalid month format. Use yyyy-MM");
        }
    }
}
