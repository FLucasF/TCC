package hotel.notifications;

import hotel.reservations.Reservation;
import hotel.rooms.Room;

/**
 * A department that wants to be notified about reservation events. Each department decides which
 * events matter and how to handle them.
 */
public interface DepartmentNotifications {

    /** Called when a reservation is confirmed (the guest paid). */
    void onConfirmation(Reservation reservation, Room room);

    /** Called when a confirmed reservation is cancelled. */
    void onCancellation(Reservation reservation, Room room);
}
