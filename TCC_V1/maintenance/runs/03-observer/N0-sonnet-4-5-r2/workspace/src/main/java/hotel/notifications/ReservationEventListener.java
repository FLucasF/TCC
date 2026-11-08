package hotel.notifications;

/** Listens to reservation events. */
public interface ReservationEventListener {
    void onEvent(ReservationEvent event);
}
