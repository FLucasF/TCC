package com.pousadameraalta.api.validation;

import com.pousadameraalta.api.dto.ReservationRequest;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;

public class RequestValidator {

    public static void validateCreateReservation(ReservationRequest req) throws ValidationException {
        if (req.roomNumber == 0) {
            throw new ValidationException("INVALID_REQUEST", "Room number is required");
        }
        if (req.guestName == null || req.guestName.trim().isEmpty()) {
            throw new ValidationException("INVALID_REQUEST", "Guest name is required");
        }
        if (req.guestEmail == null || req.guestEmail.trim().isEmpty()) {
            throw new ValidationException("INVALID_REQUEST", "Guest email is required");
        }
        if (req.checkIn == null || req.checkIn.trim().isEmpty()) {
            throw new ValidationException("INVALID_REQUEST", "Check-in date is required");
        }
        if (req.checkOut == null || req.checkOut.trim().isEmpty()) {
            throw new ValidationException("INVALID_REQUEST", "Check-out date is required");
        }
        if (req.guests == 0) {
            throw new ValidationException("INVALID_REQUEST", "Guests count is required");
        }
        if (req.ratePlan == null || req.ratePlan.trim().isEmpty()) {
            throw new ValidationException("INVALID_REQUEST", "Rate plan is required");
        }

        try {
            LocalDate.parse(req.checkIn);
            LocalDate.parse(req.checkOut);
        } catch (DateTimeParseException e) {
            throw new ValidationException("INVALID_REQUEST", "Invalid date format");
        }

        int identificationCount = 0;
        if (req.guestCpf != null && !req.guestCpf.trim().isEmpty()) identificationCount++;
        if (req.companyCnpj != null && !req.companyCnpj.trim().isEmpty()) identificationCount++;
        if (req.guestPassport != null && !req.guestPassport.trim().isEmpty()) identificationCount++;

        if (identificationCount != 1) {
            throw new ValidationException("INVALID_REQUEST", "Exactly one identification method is required");
        }

        if (req.companyCnpj != null && (req.companyName == null || req.companyName.trim().isEmpty())) {
            throw new ValidationException("INVALID_REQUEST", "Company name is required when CNPJ is provided");
        }

        if (req.guestPassport != null && (req.guestCountry == null || req.guestCountry.trim().isEmpty())) {
            throw new ValidationException("INVALID_REQUEST", "Country is required when passport is provided");
        }

        if (req.guests < 1) {
            throw new ValidationException("INVALID_REQUEST", "Guests count must be at least 1");
        }
    }

    public static void validateDate(String dateStr) throws ValidationException {
        try {
            LocalDate.parse(dateStr);
        } catch (DateTimeParseException e) {
            throw new ValidationException("INVALID_REQUEST", "Invalid date format");
        }
    }

    public static void validateMonth(String monthStr) throws ValidationException {
        try {
            String[] parts = monthStr.split("-");
            if (parts.length != 2) throw new NumberFormatException();
            int year = Integer.parseInt(parts[0]);
            int month = Integer.parseInt(parts[1]);
            if (month < 1 || month > 12) throw new NumberFormatException();
        } catch (Exception e) {
            throw new ValidationException("INVALID_REQUEST", "Invalid month format");
        }
    }

    public static class ValidationException extends Exception {
        public final String errorCode;
        public final String errorMessage;

        public ValidationException(String errorCode, String errorMessage) {
            super(errorMessage);
            this.errorCode = errorCode;
            this.errorMessage = errorMessage;
        }
    }
}
