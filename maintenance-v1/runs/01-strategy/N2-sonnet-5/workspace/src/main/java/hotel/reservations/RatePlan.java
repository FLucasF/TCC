package hotel.reservations;

import hotel.rooms.Room;
import hotel.rooms.RoomType;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Map;

/**
 * Each plan knows its own price, its own rule for when it can be used, and its own refund on
 * cancellation.
 */
public enum RatePlan {

    /** Full price; the guest gets everything back when cancelling a confirmed reservation. */
    STANDARD {
        @Override
        Price price(Room room, LocalDate checkIn, LocalDate checkOut, String companyCode, PriceCalculator calculator) {
            return calculator.nightlyPrice(room, checkIn, checkOut, BigDecimal.ZERO);
        }

        @Override
        void validate(LocalDate checkIn, LocalDate checkOut, String companyCode) {
        }

        @Override
        BigDecimal refund(BigDecimal amountPaid) {
            return amountPaid;
        }
    },

    /** 15% off, but nothing comes back on cancellation. */
    NON_REFUNDABLE {
        @Override
        Price price(Room room, LocalDate checkIn, LocalDate checkOut, String companyCode, PriceCalculator calculator) {
            return calculator.nightlyPrice(room, checkIn, checkOut, new BigDecimal("0.15"));
        }

        @Override
        void validate(LocalDate checkIn, LocalDate checkOut, String companyCode) {
        }

        @Override
        BigDecimal refund(BigDecimal amountPaid) {
            return PriceCalculator.cents(BigDecimal.ZERO);
        }
    },

    /**
     * Convênio com empresas parceiras: o hóspede informa o código da empresa (o nome dela em
     * maiúsculas) e recebe o desconto que ela negociou. Tudo volta se cancelar antes da chegada.
     */
    CORPORATE {
        @Override
        Price price(Room room, LocalDate checkIn, LocalDate checkOut, String companyCode, PriceCalculator calculator) {
            return calculator.nightlyPrice(room, checkIn, checkOut, COMPANY_DISCOUNTS.get(companyCode));
        }

        @Override
        void validate(LocalDate checkIn, LocalDate checkOut, String companyCode) {
            if (companyCode == null || !COMPANY_DISCOUNTS.containsKey(companyCode)) {
                throw new HotelException(
                        ErrorCode.INVALID_REQUEST, "the CORPORATE plan needs a known company code");
            }
        }

        @Override
        BigDecimal refund(BigDecimal amountPaid) {
            return amountPaid;
        }
    },

    /**
     * Pacote de fim de semana: entrada na sexta, saída no domingo seguinte, exatamente duas noites,
     * por um preço fechado por tipo de quarto que vale o ano inteiro. Cancelar devolve a metade.
     */
    WEEKEND_PACKAGE {
        @Override
        Price price(Room room, LocalDate checkIn, LocalDate checkOut, String companyCode, PriceCalculator calculator) {
            BigDecimal packagePrice = PACKAGE_PRICES.get(room.type());
            return new Price(2, packagePrice, PriceCalculator.cents(BigDecimal.ZERO), packagePrice);
        }

        @Override
        void validate(LocalDate checkIn, LocalDate checkOut, String companyCode) {
            boolean fridayToSunday =
                    checkIn.getDayOfWeek() == DayOfWeek.FRIDAY && checkOut.isEqual(checkIn.plusDays(2));
            if (!fridayToSunday) {
                throw new HotelException(
                        ErrorCode.INVALID_REQUEST,
                        "the WEEKEND_PACKAGE plan is only for a Friday check-in and a Sunday check-out");
            }
        }

        @Override
        BigDecimal refund(BigDecimal amountPaid) {
            return PriceCalculator.cents(amountPaid.multiply(new BigDecimal("0.5")));
        }
    },

    /**
     * Estadia longa: 7 noites ou mais, com 10% de desconto sobre as diárias (a alta temporada continua
     * valendo nas noites de temporada). Tudo volta se cancelar.
     */
    LONG_STAY {
        @Override
        Price price(Room room, LocalDate checkIn, LocalDate checkOut, String companyCode, PriceCalculator calculator) {
            return calculator.nightlyPrice(room, checkIn, checkOut, new BigDecimal("0.10"));
        }

        @Override
        void validate(LocalDate checkIn, LocalDate checkOut, String companyCode) {
            if (ChronoUnit.DAYS.between(checkIn, checkOut) < 7) {
                throw new HotelException(ErrorCode.INVALID_REQUEST, "the LONG_STAY plan needs 7 nights or more");
            }
        }

        @Override
        BigDecimal refund(BigDecimal amountPaid) {
            return amountPaid;
        }
    };

    private static final Map<String, BigDecimal> COMPANY_DISCOUNTS = Map.of(
            "TECNOSUL", new BigDecimal("0.12"),
            "AGROVALE", new BigDecimal("0.18"),
            "FARMAVIDA", new BigDecimal("0.25"));

    private static final Map<RoomType, BigDecimal> PACKAGE_PRICES = Map.of(
            RoomType.STANDARD, new BigDecimal("520.00"),
            RoomType.DELUXE, new BigDecimal("820.00"),
            RoomType.SUITE, new BigDecimal("1200.00"));

    /** The price of a stay under this plan: nights, subtotal, discount and total. */
    abstract Price price(
            Room room, LocalDate checkIn, LocalDate checkOut, String companyCode, PriceCalculator calculator);

    /** Checks whether this plan can be used for this stay; throws INVALID_REQUEST when it cannot. */
    abstract void validate(LocalDate checkIn, LocalDate checkOut, String companyCode);

    /** What comes back to the guest when a reservation paid under this plan is cancelled. */
    abstract BigDecimal refund(BigDecimal amountPaid);
}
