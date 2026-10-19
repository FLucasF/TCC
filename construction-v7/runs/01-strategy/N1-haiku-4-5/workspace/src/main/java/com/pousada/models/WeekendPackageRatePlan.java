package com.pousada.models;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;

public class WeekendPackageRatePlan extends RatePlan {
    private static final int REQUIRED_NIGHTS = 2;

    private final BigDecimal standardPrice = BigDecimal.valueOf(520.00);
    private final BigDecimal deluxePrice = BigDecimal.valueOf(820.00);
    private final BigDecimal suitePrice = BigDecimal.valueOf(1200.00);

    public WeekendPackageRatePlan() {
        super("WEEKEND_PACKAGE");
    }

    @Override
    public void validate(int roomCapacity, int guests, LocalDate checkIn, LocalDate checkOut, String companyCode) {
        if (checkIn.getDayOfWeek() != DayOfWeek.FRIDAY) {
            throw new ValidationException("INVALID_REQUEST",
                "Pacote de fim de semana deve começar na sexta-feira");
        }

        if (checkOut.getDayOfWeek() != DayOfWeek.SUNDAY ||
            !checkOut.equals(checkIn.plusDays(2))) {
            throw new ValidationException("INVALID_REQUEST",
                "Pacote de fim de semana deve ser de sexta a domingo (2 noites)");
        }

        if (guests < 1 || guests > roomCapacity) {
            throw new ValidationException("CAPACITY_EXCEEDED",
                "Número de hóspedes inválido para a capacidade do quarto");
        }
    }

    @Override
    public BigDecimal calculateSubtotal(BigDecimal nightlyRate, int nights, LocalDate checkIn, LocalDate checkOut, Room.RoomType roomType) {
        return switch (roomType) {
            case STANDARD -> standardPrice;
            case DELUXE -> deluxePrice;
            case SUITE -> suitePrice;
        };
    }

    @Override
    public BigDecimal calculateDiscount(BigDecimal subtotal) {
        return BigDecimal.ZERO;
    }

    @Override
    public BigDecimal calculateRefund(Reservation reservation) {
        if (reservation.getStatus() == Reservation.ReservationStatus.PENDING) {
            return BigDecimal.ZERO;
        }
        return PricingUtils.roundHalfEven(reservation.getAmountPaid().multiply(BigDecimal.valueOf(0.5)));
    }
}
