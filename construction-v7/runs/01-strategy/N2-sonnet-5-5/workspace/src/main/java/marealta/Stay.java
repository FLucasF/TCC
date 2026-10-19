package marealta;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/** O que um plano de tarifa precisa conhecer da estadia pedida. */
public record Stay(Room room, LocalDate checkIn, LocalDate checkOut, String companyCode) {
    public int nights() {
        return (int) ChronoUnit.DAYS.between(checkIn, checkOut);
    }
}
