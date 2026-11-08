package hotel.notifications;

import hotel.reservations.Reservation;
import hotel.rooms.Room;
import java.util.List;

/**
 * Coordinates notifications to all departments interested in reservation events. When a new
 * department wants to be notified, add it to the list.
 */
public final class ReservationNotifications {

    private final List<DepartmentNotifications> departments;

    public ReservationNotifications(List<DepartmentNotifications> departments) {
        this.departments = departments;
    }

    public void onConfirmation(Reservation reservation, Room room) {
        for (DepartmentNotifications department : departments) {
            department.onConfirmation(reservation, room);
        }
    }

    public void onCancellation(Reservation reservation, Room room) {
        for (DepartmentNotifications department : departments) {
            department.onCancellation(reservation, room);
        }
    }
}
