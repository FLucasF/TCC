package hotel.reservations;

import java.time.LocalDate;

/** A period of the year, which may change from year to year, that is part of the high season. */
interface Season {

    boolean includes(LocalDate night);
}
