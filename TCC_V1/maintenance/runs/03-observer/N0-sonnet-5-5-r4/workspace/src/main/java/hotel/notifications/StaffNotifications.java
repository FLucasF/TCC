package hotel.notifications;

import hotel.messages.EmailSender;
import hotel.reservations.ReservationListener;
import java.util.List;

/** The staff who are told about reservations. To add or remove someone, change this list. */
public final class StaffNotifications {

    private StaffNotifications() {
    }

    public static List<ReservationListener> all(EmailSender emailSender) {
        return List.of(new GovernanceNotifier(emailSender), new FinanceNotifier(emailSender));
    }
}
