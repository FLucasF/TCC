package com.pousada.domain.rateplans;

import com.pousada.domain.Money;
import com.pousada.domain.Room;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Map;

public class WeekendPackageRatePlan extends BaseRatePlan {
    private static final Map<Room.RoomType, BigDecimal> PACKAGE_PRICES = Map.of(
        Room.RoomType.STANDARD, new BigDecimal("520.00"),
        Room.RoomType.DELUXE, new BigDecimal("820.00"),
        Room.RoomType.SUITE, new BigDecimal("1200.00")
    );

    @Override
    public String getId() {
        return "WEEKEND_PACKAGE";
    }

    @Override
    public void validate(int roomNumber, LocalDate checkIn, LocalDate checkOut, int guests, String companyCode) {
        validateBasic(guests);

        if (checkIn.getDayOfWeek() != DayOfWeek.FRIDAY) {
            throw new InvalidReservationException("INVALID_REQUEST", "Pacote de fim de semana deve começar numa sexta");
        }

        LocalDate expectedCheckOut = checkIn.plusDays(2);
        if (!checkOut.equals(expectedCheckOut)) {
            throw new InvalidReservationException("INVALID_REQUEST", "Pacote de fim de semana deve ter exatamente 2 noites");
        }

        if (checkOut.getDayOfWeek() != DayOfWeek.SUNDAY) {
            throw new InvalidReservationException("INVALID_REQUEST", "Pacote de fim de semana deve terminar num domingo");
        }
    }

    @Override
    public PriceCalculation calculatePrice(Room room, LocalDate checkIn, LocalDate checkOut) {
        BigDecimal price = PACKAGE_PRICES.get(room.getType());
        return new PriceCalculation(price, BigDecimal.ZERO, price);
    }

    @Override
    public BigDecimal calculateRefund(BigDecimal amountPaid) {
        return Money.round(amountPaid.multiply(new BigDecimal("0.5")));
    }
}
