package hotel.notifications;

import hotel.messages.EmailSender;
import java.util.List;

/** The listeners the inn has today. To add or remove a recipient, change this list. */
public final class ReservationListeners {

    private ReservationListeners() {
    }

    public static List<ReservationListener> standard(EmailSender emailSender) {
        return List.of(
                new GuestNotifications(emailSender),
                new HousekeepingNotifications(emailSender),
                new FinanceNotifications(emailSender));
    }
}
