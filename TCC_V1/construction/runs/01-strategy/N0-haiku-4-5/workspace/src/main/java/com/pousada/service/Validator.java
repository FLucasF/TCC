package com.pousada.service;

import java.time.LocalDate;
import java.time.DayOfWeek;

public class Validator {
    public static class ValidationError {
        public String code;
        public String message;

        public ValidationError(String code, String message) {
            this.code = code;
            this.message = message;
        }
    }

    public static ValidationError validateReservationRequest(
            int roomNumber, LocalDate checkIn, LocalDate checkOut, int guests, String ratePlan, String companyCode, int roomCapacity) {

        if (checkOut.isBefore(checkIn) || checkOut.equals(checkIn)) {
            return new ValidationError("INVALID_REQUEST", "Check-out date must be after check-in date");
        }

        if (guests < 1) {
            return new ValidationError("INVALID_REQUEST", "At least 1 guest is required");
        }

        if (guests > roomCapacity) {
            return new ValidationError("CAPACITY_EXCEEDED", "Number of guests exceeds room capacity");
        }

        if (ratePlan == null || ratePlan.isEmpty()) {
            return new ValidationError("INVALID_REQUEST", "Rate plan is required");
        }

        if (!isValidRatePlan(ratePlan)) {
            return new ValidationError("INVALID_REQUEST", "Invalid rate plan: " + ratePlan);
        }

        if ("CORPORATE".equals(ratePlan)) {
            if (companyCode == null || companyCode.isEmpty()) {
                return new ValidationError("INVALID_REQUEST", "Company code is required for CORPORATE rate plan");
            }
            if (!isValidCompanyCode(companyCode)) {
                return new ValidationError("INVALID_REQUEST", "Invalid company code: " + companyCode);
            }
        }

        if ("WEEKEND_PACKAGE".equals(ratePlan)) {
            ValidationError weekendError = validateWeekendPackage(checkIn, checkOut);
            if (weekendError != null) {
                return weekendError;
            }
        }

        if ("LONG_STAY".equals(ratePlan)) {
            long nights = checkIn.until(checkOut).getDays();
            if (nights < 7) {
                return new ValidationError("INVALID_REQUEST", "Long stay requires at least 7 nights");
            }
        }

        return null;
    }

    private static ValidationError validateWeekendPackage(LocalDate checkIn, LocalDate checkOut) {
        if (checkIn.getDayOfWeek() != DayOfWeek.FRIDAY) {
            return new ValidationError("INVALID_REQUEST", "Weekend package must start on Friday");
        }

        if (checkOut.getDayOfWeek() != DayOfWeek.MONDAY) {
            return new ValidationError("INVALID_REQUEST", "Weekend package must end on Sunday (checkout Monday)");
        }

        long nights = checkIn.until(checkOut).getDays();
        if (nights != 2) {
            return new ValidationError("INVALID_REQUEST", "Weekend package must be exactly 2 nights");
        }

        return null;
    }

    private static boolean isValidRatePlan(String ratePlan) {
        return switch (ratePlan) {
            case "STANDARD", "NON_REFUNDABLE", "CORPORATE", "WEEKEND_PACKAGE", "LONG_STAY" -> true;
            default -> false;
        };
    }

    private static boolean isValidCompanyCode(String companyCode) {
        return switch (companyCode.toUpperCase()) {
            case "TECNOSUL", "AGROVALE", "FARMAVIDA" -> true;
            default -> false;
        };
    }
}
