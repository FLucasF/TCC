package hotel.notifications;

import hotel.messages.EmailSender;
import hotel.reservations.ReservationListener;
import java.util.List;

/** Who is told about reservations today. To add or remove an interested party, change this list. */
public final class DefaultListeners {

    private DefaultListeners() {
    }

    public static List<ReservationListener> create(EmailSender emailSender) {
        return List.of(
                new GuestNotifier(emailSender),
                new HousekeepingNotifier(emailSender),
                new FinanceNotifier(emailSender));
    }
}
