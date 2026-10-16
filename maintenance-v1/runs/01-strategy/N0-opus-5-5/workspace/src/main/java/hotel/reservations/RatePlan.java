package hotel.reservations;

import hotel.rooms.Room;
import hotel.rooms.RoomType;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.util.Map;

/**
 * The rate plans the guest can book. Each plan has its own terms: who may use it, what the stay costs,
 * and how much comes back when a confirmed reservation is cancelled. A new plan is a new constant here.
 */
public enum RatePlan {

    /** Full price; the guest gets everything back when cancelling a confirmed reservation. */
    STANDARD {
        @Override
        BigDecimal refund(BigDecimal amountPaid) {
            return amountPaid;
        }
    },

    /** 15% off, but nothing comes back on cancellation. */
    NON_REFUNDABLE {
        @Override
        BigDecimal discountRate(String companyCode) {
            return new BigDecimal("0.15");
        }

        @Override
        BigDecimal refund(BigDecimal amountPaid) {
            return PriceCalculator.cents(BigDecimal.ZERO);
        }
    },

    /** The discount agreed with the guest's company, which must be one we have an agreement with; full refund. */
    CORPORATE {
        @Override
        void checkEligibility(NewReservation request) {
            discountRate(request.companyCode());
        }

        @Override
        BigDecimal discountRate(String companyCode) {
            BigDecimal discount = companyCode == null ? null : COMPANY_DISCOUNTS.get(companyCode);
            if (discount == null) {
                throw new HotelException(ErrorCode.INVALID_REQUEST,
                        "the CORPORATE rate plan needs the code of a company with an agreement");
            }
            return discount;
        }

        @Override
        BigDecimal refund(BigDecimal amountPaid) {
            return amountPaid;
        }
    },

    /**
     * Friday to Sunday, exactly two nights, for a fixed price per room type the whole year (no high
     * season, no discount); half comes back on cancellation.
     */
    WEEKEND_PACKAGE {
        @Override
        void checkEligibility(NewReservation request) {
            boolean fridayToSunday = request.checkIn().getDayOfWeek() == DayOfWeek.FRIDAY
                    && request.checkOut().equals(request.checkIn().plusDays(2));
            if (!fridayToSunday) {
                throw new HotelException(ErrorCode.INVALID_REQUEST,
                        "the WEEKEND_PACKAGE rate plan is only from a Friday to the Sunday right after it");
            }
        }

        @Override
        Price price(Room room, int nights, BigDecimal nightlySubtotal, String companyCode) {
            BigDecimal packagePrice = PriceCalculator.cents(WEEKEND_PACKAGE_PRICES.get(room.type()));
            return new Price(nights, packagePrice, PriceCalculator.cents(BigDecimal.ZERO), packagePrice);
        }

        @Override
        BigDecimal refund(BigDecimal amountPaid) {
            return PriceCalculator.cents(amountPaid.multiply(new BigDecimal("0.5")));
        }
    },

    /** 7 nights or more: 10% off the nights (high season still applies); full refund. */
    LONG_STAY {
        @Override
        void checkEligibility(NewReservation request) {
            if (request.checkIn().plusDays(LONG_STAY_MINIMUM_NIGHTS).isAfter(request.checkOut())) {
                throw new HotelException(ErrorCode.INVALID_REQUEST,
                        "the LONG_STAY rate plan needs at least " + LONG_STAY_MINIMUM_NIGHTS + " nights");
            }
        }

        @Override
        BigDecimal discountRate(String companyCode) {
            return new BigDecimal("0.10");
        }

        @Override
        BigDecimal refund(BigDecimal amountPaid) {
            return amountPaid;
        }
    };

    /** The companies with an agreement, by code (the company's name in capitals), and their discount. */
    static final Map<String, BigDecimal> COMPANY_DISCOUNTS = Map.of(
            "TECNOSUL", new BigDecimal("0.12"),
            "AGROVALE", new BigDecimal("0.18"),
            "FARMAVIDA", new BigDecimal("0.25"));

    static final Map<RoomType, BigDecimal> WEEKEND_PACKAGE_PRICES = Map.of(
            RoomType.STANDARD, new BigDecimal("520.00"),
            RoomType.DELUXE, new BigDecimal("820.00"),
            RoomType.SUITE, new BigDecimal("1200.00"));

    static final int LONG_STAY_MINIMUM_NIGHTS = 7;

    /** Refuses (INVALID_REQUEST) a stay that does not meet the plan's conditions. */
    void checkEligibility(NewReservation request) {
    }

    /** The price of the stay, given the sum of its nights (high season included). */
    Price price(Room room, int nights, BigDecimal nightlySubtotal, String companyCode) {
        BigDecimal discount = PriceCalculator.cents(nightlySubtotal.multiply(discountRate(companyCode)));
        return new Price(nights, nightlySubtotal, discount, nightlySubtotal.subtract(discount));
    }

    /** The share of the nights taken off the price. */
    BigDecimal discountRate(String companyCode) {
        return BigDecimal.ZERO;
    }

    /** What comes back when a reservation is cancelled after the guest paid this amount. */
    abstract BigDecimal refund(BigDecimal amountPaid);
}
