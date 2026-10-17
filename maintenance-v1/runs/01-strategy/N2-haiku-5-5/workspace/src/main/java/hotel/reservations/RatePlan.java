package hotel.reservations;

import hotel.rooms.Room;
import hotel.rooms.RoomType;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.util.Map;

/** Each plan decides who can use it, the subtotal and discount of a stay, and what comes back when cancelling. */
public enum RatePlan {

    /** Full price; the guest gets everything back when cancelling a confirmed reservation. */
    STANDARD,

    /** 15% off, but nothing comes back on cancellation. */
    NON_REFUNDABLE {
        @Override
        BigDecimal discount(NewReservation request, BigDecimal subtotal) {
            return PriceCalculator.percentOf(subtotal, new BigDecimal("0.15"));
        }

        @Override
        BigDecimal refund(BigDecimal amountPaid) {
            return PriceCalculator.cents(BigDecimal.ZERO);
        }
    },

    /** The discount a company agreed with the inn; the guest must give the company's code. */
    CORPORATE {
        @Override
        void checkEligibility(NewReservation request) {
            if (request.companyCode() == null || !COMPANY_DISCOUNTS.containsKey(request.companyCode())) {
                throw new HotelException(ErrorCode.INVALID_REQUEST, "a corporate reservation needs a valid company code");
            }
        }

        @Override
        BigDecimal discount(NewReservation request, BigDecimal subtotal) {
            return PriceCalculator.percentOf(subtotal, COMPANY_DISCOUNTS.get(request.companyCode()));
        }
    },

    /** A fixed price for two nights from Friday to Sunday, in any season. Half comes back on cancellation. */
    WEEKEND_PACKAGE {
        @Override
        void checkEligibility(NewReservation request) {
            boolean friday = request.checkIn().getDayOfWeek() == DayOfWeek.FRIDAY;
            if (!friday || PriceCalculator.nights(request.checkIn(), request.checkOut()) != 2) {
                throw new HotelException(
                        ErrorCode.INVALID_REQUEST, "the weekend package is two nights, from a Friday to the Sunday after");
            }
        }

        @Override
        BigDecimal subtotal(NewReservation request, Room room) {
            return WEEKEND_PRICES.get(room.type());
        }

        @Override
        BigDecimal discount(NewReservation request, BigDecimal subtotal) {
            return PriceCalculator.cents(BigDecimal.ZERO);
        }

        @Override
        BigDecimal refund(BigDecimal amountPaid) {
            return PriceCalculator.percentOf(amountPaid, new BigDecimal("0.50"));
        }
    },

    /** 10% off for stays of seven nights or more. Everything comes back on cancellation. */
    LONG_STAY {
        @Override
        void checkEligibility(NewReservation request) {
            if (PriceCalculator.nights(request.checkIn(), request.checkOut()) < 7) {
                throw new HotelException(ErrorCode.INVALID_REQUEST, "the long stay needs at least seven nights");
            }
        }

        @Override
        BigDecimal discount(NewReservation request, BigDecimal subtotal) {
            return PriceCalculator.percentOf(subtotal, new BigDecimal("0.10"));
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

    void checkEligibility(NewReservation request) {
    }

    BigDecimal subtotal(NewReservation request, Room room) {
        return PriceCalculator.nightsTotal(room, request.checkIn(), request.checkOut());
    }

    BigDecimal discount(NewReservation request, BigDecimal subtotal) {
        return PriceCalculator.cents(BigDecimal.ZERO);
    }

    BigDecimal refund(BigDecimal amountPaid) {
        return amountPaid;
    }
}
