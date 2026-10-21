package pousada;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Month;

public final class Season {
    private Season() {}

    public static boolean isHigh(LocalDate night) {
        Month month = night.getMonth();
        return month == Month.JULY || month == Month.JANUARY || month == Month.FEBRUARY
                || (month == Month.DECEMBER && night.getDayOfMonth() >= 15);
    }

    public static BigDecimal nightPrice(Room room, LocalDate night) {
        return isHigh(night) ? Money.fraction(room.nightlyRate(), "1.20") : room.nightlyRate();
    }
}
