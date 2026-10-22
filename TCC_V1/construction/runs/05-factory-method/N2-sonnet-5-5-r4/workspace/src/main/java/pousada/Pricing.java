package pousada;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Month;

record Pricing(int nights, BigDecimal subtotal, BigDecimal discount, BigDecimal total) {

    static Pricing quote(Room room, LocalDate checkIn, LocalDate checkOut, RatePlan plan) {
        BigDecimal subtotal = Money.round(BigDecimal.ZERO);
        int nights = 0;
        for (LocalDate night = checkIn; night.isBefore(checkOut); night = night.plusDays(1)) {
            subtotal = subtotal.add(nightPrice(room.nightlyRate(), night));
            nights++;
        }
        BigDecimal discount = plan.discountOn(subtotal);
        return new Pricing(nights, subtotal, discount, subtotal.subtract(discount));
    }

    static BigDecimal nightPrice(BigDecimal rate, LocalDate night) {
        return isHighSeason(night) ? Money.percent(rate, "1.20") : Money.round(rate);
    }

    static boolean isHighSeason(LocalDate night) {
        Month month = night.getMonth();
        return month == Month.JANUARY || month == Month.FEBRUARY || month == Month.JULY
                || (month == Month.DECEMBER && night.getDayOfMonth() >= 15);
    }
}
