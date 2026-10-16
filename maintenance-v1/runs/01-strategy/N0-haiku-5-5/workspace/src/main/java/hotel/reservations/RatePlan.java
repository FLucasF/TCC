package hotel.reservations;

import hotel.rooms.Room;
import hotel.rooms.RoomType;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.Optional;

/** Each plan decides who may book it, how the stay is priced, and what comes back on cancellation. */
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
    /** The discount agreed with a company; the company code says which one. */
    CORPORATE {
        @Override
        void checkRequest(NewReservation request) {
            companyDiscount(request);
        }

        @Override
        BigDecimal discountRate(NewReservation request) {
            return companyDiscount(request);
        }
    },
    /** Friday to Sunday, exactly two nights, one fixed price all year. Half comes back on cancellation. */
    WEEKEND_PACKAGE {
        @Override
        void checkRequest(NewReservation request) {
            if (request.checkIn().getDayOfWeek() != DayOfWeek.FRIDAY || nights(request) != 2) {
                throw new HotelException(
                        ErrorCode.INVALID_REQUEST, "the weekend package is for two nights from a Friday to a Sunday");
            }
        }

        @Override
        Price price(Room room, NewReservation request) {
            BigDecimal packagePrice = WEEKEND_PRICES.get(room.type());
            return new Price(2, packagePrice, PriceCalculator.cents(BigDecimal.ZERO), packagePrice);
        }

        @Override
        BigDecimal refund(BigDecimal amountPaid) {
            return PriceCalculator.cents(amountPaid.multiply(new BigDecimal("0.5")));
        }
    },
    /** 10% off the nights, for seven nights or more. */
    LONG_STAY {
        @Override
        void checkRequest(NewReservation request) {
            if (nights(request) < 7) {
                throw new HotelException(ErrorCode.INVALID_REQUEST, "the long stay needs at least seven nights");
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

    private static final Map<RoomType, BigDecimal> WEEKEND_PRICES = Map.of(
            RoomType.STANDARD, new BigDecimal("520.00"),
            RoomType.DELUXE, new BigDecimal("820.00"),
            RoomType.SUITE, new BigDecimal("1200.00"));

    void checkRequest(NewReservation request) {
    }

    BigDecimal discountRate(NewReservation request) {
        return BigDecimal.ZERO;
    }

    Price price(Room room, NewReservation request) {
        return PriceCalculator.stay(room, request.checkIn(), request.checkOut(), discountRate(request));
    }

    BigDecimal refund(BigDecimal amountPaid) {
        return amountPaid;
    }

    private static long nights(NewReservation request) {
        return ChronoUnit.DAYS.between(request.checkIn(), request.checkOut());
    }

    private static BigDecimal companyDiscount(NewReservation request) {
        return Optional.ofNullable(request.companyCode())
                .map(COMPANY_DISCOUNTS::get)
                .orElseThrow(() -> new HotelException(
                        ErrorCode.INVALID_REQUEST, "the corporate plan needs the code of a company with an agreement"));
    }
}
