package hotel.reservations.notifications;

import hotel.messages.EmailSender;
import java.util.ArrayList;
import java.util.List;

public final class ReservationNotifiers {
    private final EmailSender emailSender;
    private final List<ReservationEventNotifier> notifiers;

    public ReservationNotifiers(EmailSender emailSender) {
        this.emailSender = emailSender;
        this.notifiers = new ArrayList<>();
    }

    public ReservationNotifiers addHousekeeping(String email) {
        notifiers.add(new HousekeepingNotifier(emailSender, email));
        return this;
    }

    public ReservationNotifiers addFinance(String email) {
        notifiers.add(new FinanceNotifier(emailSender, email));
        return this;
    }

    public List<ReservationEventNotifier> build() {
        return new ArrayList<>(notifiers);
    }
}
