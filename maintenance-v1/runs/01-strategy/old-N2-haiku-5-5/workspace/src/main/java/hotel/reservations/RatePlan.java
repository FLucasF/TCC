package hotel.reservations;

import hotel.rooms.Room;
import hotel.rooms.RoomType;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.temporal.ChronoUnit;
import java.util.Map;

/** Each plan has its own price, its own rules for who can book it, and its own refund on cancellation. */
public enum RatePlan {
    /** Full price; the guest gets everything back when cancelling a confirmed reservation. */
    STANDARD,

    /** 15% off, but nothing comes back on cancellation. */
    NON_REFUNDABLE {
        @Override
        BigDecimal discountRate(NewReservation request) {
            return new BigDecimal("0.15");
        }

        @Override
        BigDecimal refund(BigDecimal amountPaid) {
            return PriceCalculator.cents(BigDecimal.ZERO);
        }
    },

    /** Discount for the companies with an agreement; the company code tells which one. */
    CORPORATE {
        @Override
        void checkStay(NewReservation request) {
            if (companyDiscount(request.companyCode()) == null) {
                throw new HotelException(
                        ErrorCode.INVALID_REQUEST,
                        "the corporate rate needs the code of a company with an agreement "
                                + "(TECNOSUL, AGROVALE or FARMAVIDA)");
            }
        }

        @Override
        BigDecimal discountRate(NewReservation request) {
            return companyDiscount(request.companyCode());
        }
    },

    /** A fixed price for a Friday-to-Sunday stay of exactly two nights, all year round. */
    WEEKEND_PACKAGE {
        @Override
        void checkStay(NewReservation request) {
            boolean fridayToSunday = request.checkIn().getDayOfWeek() == DayOfWeek.FRIDAY
                    && request.checkOut().equals(request.checkIn().plusDays(2));
            if (!fridayToSunday) {
                throw new HotelException(
                        ErrorCode.INVALID_REQUEST, "the weekend package is for a Friday check-in and a Sunday check-out");
            }
        }

        @Override
        Price price(Room room, NewReservation request) {
            BigDecimal packagePrice = switch (room.type()) {
                case STANDARD -> new BigDecimal("520.00");
                case DELUXE -> new BigDecimal("820.00");
                case SUITE -> new BigDecimal("1200.00");
            };
            return new Price(2, packagePrice, PriceCalculator.cents(BigDecimal.ZERO), packagePrice);
        }

        @Override
        BigDecimal refund(BigDecimal amountPaid) {
            return PriceCalculator.cents(amountPaid.multiply(new BigDecimal("0.5")));
        }
    },

    /** 10% off the nights, for stays of seven nights or more. */
    LONG_STAY {
        @Override
        void checkStay(NewReservation request) {
            if (ChronoUnit.DAYS.between(request.checkIn(), request.checkOut()) < 7) {
                throw new HotelException(ErrorCode.INVALID_REQUEST, "the long stay rate needs at least 7 nights");
            }
        }

        @Override
        BigDecimal discountRate(NewReservation request) {
            return new BigDecimal("0.10");
        }
    };

    private static final Map<String, BigDecimal> COMPANY_DISCOUNTS = Map.of(
            "TECNOSUL", new BigDecimal("0.12"),
            "AGROVALE", new BigDecimal("0.18"),
            "FARMAVIDA", new BigDecimal("0.25"));

    void checkStay(NewReservation request) {
    }

    Price price(Room room, NewReservation request) {
        return PriceCalculator.perNight(room, request.checkIn(), request.checkOut(), discountRate(request));
    }

    BigDecimal discountRate(NewReservation request) {
        return BigDecimal.ZERO;
    }

    BigDecimal refund(BigDecimal amountPaid) {
        return amountPaid;
    }

    private static BigDecimal companyDiscount(String companyCode) {
        return companyCode == null ? null : COMPANY_DISCOUNTS.get(companyCode);
    }
}
