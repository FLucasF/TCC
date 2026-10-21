package hotel.reservations;

/** Someone who wants to know what happens to reservations. Each listener picks the events it cares about. */
public interface ReservationListener {

    void on(ReservationEvent event);
}
