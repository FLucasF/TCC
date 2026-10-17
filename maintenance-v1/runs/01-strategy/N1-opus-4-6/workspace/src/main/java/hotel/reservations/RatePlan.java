package hotel.reservations;

import hotel.rooms.Room;
import hotel.rooms.RoomType;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Map;

public enum RatePlan {

    STANDARD {
        @Override
        void validate(LocalDate checkIn, LocalDate checkOut, Room room, String companyCode) {}

        @Override
        Price calculatePrice(Room room, LocalDate checkIn, LocalDate checkOut, String companyCode) {
            return standardPrice(room, checkIn, checkOut, BigDecimal.ZERO);
        }

        @Override
        BigDecimal refund(BigDecimal amountPaid) {
            return amountPaid;
        }
    },

    NON_REFUNDABLE {
        private static final BigDecimal DISCOUNT_RATE = new BigDecimal("0.15");

        @Override
        void validate(LocalDate checkIn, LocalDate checkOut, Room room, String companyCode) {}

        @Override
        Price calculatePrice(Room room, LocalDate checkIn, LocalDate checkOut, String companyCode) {
            return standardPrice(room, checkIn, checkOut, DISCOUNT_RATE);
        }

        @Override
        BigDecimal refund(BigDecimal amountPaid) {
            return cents(BigDecimal.ZERO);
        }
    },

    CORPORATE {
        private static final Map<String, BigDecimal> COMPANY_DISCOUNTS = Map.of(
                "TECNOSUL", new BigDecimal("0.12"),
                "AGROVALE", new BigDecimal("0.18"),
                "FARMAVIDA", new BigDecimal("0.25"));

        @Override
        void validate(LocalDate checkIn, LocalDate checkOut, Room room, String companyCode) {
            if (companyCode == null || !COMPANY_DISCOUNTS.containsKey(companyCode)) {
                throw new HotelException(
                        ErrorCode.INVALID_REQUEST, "a valid company code is required for the corporate plan");
            }
        }

        @Override
        Price calculatePrice(Room room, LocalDate checkIn, LocalDate checkOut, String companyCode) {
            return standardPrice(room, checkIn, checkOut, COMPANY_DISCOUNTS.get(companyCode));
        }

        @Override
        BigDecimal refund(BigDecimal amountPaid) {
            return amountPaid;
        }
    },

    WEEKEND_PACKAGE {
        private static final Map<RoomType, BigDecimal> PACKAGE_PRICES = Map.of(
                RoomType.STANDARD, new BigDecimal("520.00"),
                RoomType.DELUXE, new BigDecimal("820.00"),
                RoomType.SUITE, new BigDecimal("1200.00"));

        @Override
        void validate(LocalDate checkIn, LocalDate checkOut, Room room, String companyCode) {
            if (checkIn.getDayOfWeek() != DayOfWeek.FRIDAY || !checkOut.equals(checkIn.plusDays(2))) {
                throw new HotelException(
                        ErrorCode.INVALID_REQUEST,
                        "the weekend package requires check-in on Friday and check-out on Sunday");
            }
        }

        @Override
        Price calculatePrice(Room room, LocalDate checkIn, LocalDate checkOut, String companyCode) {
            BigDecimal price = PACKAGE_PRICES.get(room.type());
            return new Price(2, price, cents(BigDecimal.ZERO), price);
        }

        @Override
        BigDecimal refund(BigDecimal amountPaid) {
            return cents(amountPaid.multiply(new BigDecimal("0.50")));
        }
    },

    LONG_STAY {
        private static final BigDecimal DISCOUNT_RATE = new BigDecimal("0.10");
        private static final int MIN_NIGHTS = 7;

        @Override
        void validate(LocalDate checkIn, LocalDate checkOut, Room room, String companyCode) {
            long nights = java.time.temporal.ChronoUnit.DAYS.between(checkIn, checkOut);
            if (nights < MIN_NIGHTS) {
                throw new HotelException(
                        ErrorCode.INVALID_REQUEST,
                        "the long stay plan requires at least " + MIN_NIGHTS + " nights");
            }
        }

        @Override
        Price calculatePrice(Room room, LocalDate checkIn, LocalDate checkOut, String companyCode) {
            return standardPrice(room, checkIn, checkOut, DISCOUNT_RATE);
        }

        @Override
        BigDecimal refund(BigDecimal amountPaid) {
            return amountPaid;
        }
    };

    abstract void validate(LocalDate checkIn, LocalDate checkOut, Room room, String companyCode);

    abstract Price calculatePrice(Room room, LocalDate checkIn, LocalDate checkOut, String companyCode);

    abstract BigDecimal refund(BigDecimal amountPaid);

    static Price standardPrice(Room room, LocalDate checkIn, LocalDate checkOut, BigDecimal discountRate) {
        int nights = 0;
        BigDecimal subtotal = cents(BigDecimal.ZERO);
        for (LocalDate night = checkIn; night.isBefore(checkOut); night = night.plusDays(1)) {
            subtotal = subtotal.add(PriceCalculator.nightPrice(room, night));
            nights++;
        }
        BigDecimal discount = cents(subtotal.multiply(discountRate));
        return new Price(nights, subtotal, discount, subtotal.subtract(discount));
    }

    static BigDecimal cents(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_EVEN);
    }
}
