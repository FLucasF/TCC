package hotel.notifications;

import hotel.messages.EmailSender;
import java.util.List;

/** Who is told about reservations. To add or remove someone, change this list only. */
public final class ReservationListeners {

    private ReservationListeners() {
    }

    public static List<ReservationListener> standard(EmailSender emailSender) {
        return List.of(
                new GuestNotice(emailSender),
                new HousekeepingNotice(emailSender, "governanca@pousadamarealta.com.br"),
                new FinanceNotice(emailSender, "financeiro@pousadamarealta.com.br"));
    }
}
