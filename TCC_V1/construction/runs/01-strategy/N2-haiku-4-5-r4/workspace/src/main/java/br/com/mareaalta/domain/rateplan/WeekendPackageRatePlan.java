package br.com.mareaalta.domain.rateplan;

import br.com.mareaalta.domain.Room;
import br.com.mareaalta.domain.RoomType;
import br.com.mareaalta.util.DateUtils;
import br.com.mareaalta.util.Money;
import java.time.LocalDate;
import java.util.Map;

public class WeekendPackageRatePlan implements RatePlan {
    private static final Map<RoomType, Double> PACKAGE_PRICES = Map.of(
            RoomType.STANDARD, 520.00,
            RoomType.DELUXE, 820.00,
            RoomType.SUITE, 1200.00
    );

    @Override
    public ValidationError validate(LocalDate checkIn, LocalDate checkOut, int guests, Room room, String companyCode) {
        if (!DateUtils.isFriday(checkIn)) {
            return ValidationError.invalid("INVALID_REQUEST", "Entrada deve ser numa sexta-feira");
        }

        LocalDate expectedCheckOut = DateUtils.nextSunday(checkIn);
        if (!checkOut.equals(expectedCheckOut)) {
            return ValidationError.invalid("INVALID_REQUEST", "Saída deve ser no domingo imediatamente após");
        }

        if (guests < 1) {
            return ValidationError.invalid("INVALID_REQUEST", "Mínimo de 1 hóspede");
        }
        if (guests > room.getCapacity()) {
            return ValidationError.invalid("CAPACITY_EXCEEDED", "Número de hóspedes excede a capacidade");
        }

        return null;
    }

    @Override
    public PricingResult calculatePrice(LocalDate checkIn, LocalDate checkOut, Room room) {
        double price = PACKAGE_PRICES.get(room.getType());
        return new PricingResult(price, 0.0, price);
    }

    @Override
    public double getRefundPercentage() {
        return 50.0;
    }
}
