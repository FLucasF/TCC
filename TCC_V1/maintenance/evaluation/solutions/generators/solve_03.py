"""Scenario 03 (Observer): the pattern solution (the service tells listeners; guest, housekeeping and finance are
listeners wired in Hotel) and the naive one (the service sends the three e-mails itself)."""

from kit import HOTEL, HTTP_TEST, REPORTS_TEST, SERVICE, SERVICE_TEST, Solution

HOUSEKEEPING_BODY_CONFIRMED = """"Quarto " + room.number() + " (" + room.type() + ")\\n"
                + "Entrada: " + TextFormat.date(reservation.checkIn())
                + " | Saída: " + TextFormat.date(reservation.checkOut()) + "\\n"
                + "Hóspedes: " + reservation.guests() + "\\n\""""

# ---------------------------------------------------------------- the pattern: listeners
pattern = Solution("03-pattern")
pattern.write("src/main/java/hotel/reservations/ReservationListener.java", """package hotel.reservations;

import hotel.rooms.Room;

/** Someone who wants to know what happens to reservations. Registered when the inn is put together (Hotel). */
public interface ReservationListener {

    /** The guest paid and the reservation is confirmed. */
    default void confirmed(Reservation reservation, Room room) {
    }

    /** A confirmed reservation was cancelled (the refund is already in the reservation). */
    default void cancelled(Reservation reservation, Room room) {
    }
}
""")
pattern.write("src/main/java/hotel/notifications/GuestConfirmationEmail.java", """package hotel.notifications;

import hotel.documents.ReservationReceipt;
import hotel.messages.EmailSender;
import hotel.reservations.Reservation;
import hotel.reservations.ReservationListener;
import hotel.rooms.Room;

/** The guest gets the receipt by e-mail when the reservation is confirmed. */
public final class GuestConfirmationEmail implements ReservationListener {

    static final String HOTEL_NAME = "Pousada Maré Alta";

    private final EmailSender emailSender;

    public GuestConfirmationEmail(EmailSender emailSender) {
        this.emailSender = emailSender;
    }

    @Override
    public void confirmed(Reservation reservation, Room room) {
        ReservationReceipt receipt = new ReservationReceipt(reservation, room);
        emailSender.send(reservation.guest().email(), "Reserva confirmada - " + HOTEL_NAME, body(reservation, receipt));
    }

    private static String body(Reservation reservation, ReservationReceipt receipt) {
        return "Olá, " + reservation.guest().name() + "!\\n\\n"
                + "Sua reserva na " + HOTEL_NAME + " está confirmada. Estes são os dados dela:\\n\\n"
                + receipt.text() + "\\n"
                + "A entrada é a partir das 14h, e a saída, até as 12h.\\n"
                + "Até breve!\\n";
    }
}
""")
pattern.write("src/main/java/hotel/notifications/HousekeepingNotice.java", f"""package hotel.notifications;

import hotel.documents.TextFormat;
import hotel.messages.EmailSender;
import hotel.reservations.Reservation;
import hotel.reservations.ReservationListener;
import hotel.rooms.Room;

/** Housekeeping prepares the room for confirmed reservations and frees it when one is cancelled. */
public final class HousekeepingNotice implements ReservationListener {{

    static final String HOUSEKEEPING_EMAIL = "governanca@pousadamarealta.com.br";

    private final EmailSender emailSender;

    public HousekeepingNotice(EmailSender emailSender) {{
        this.emailSender = emailSender;
    }}

    @Override
    public void confirmed(Reservation reservation, Room room) {{
        emailSender.send(HOUSEKEEPING_EMAIL, "Reserva confirmada - quarto " + room.number(),
                {HOUSEKEEPING_BODY_CONFIRMED});
    }}

    @Override
    public void cancelled(Reservation reservation, Room room) {{
        emailSender.send(HOUSEKEEPING_EMAIL, "Reserva cancelada - quarto " + room.number(),
                "O quarto " + room.number() + " ficou livre de " + TextFormat.date(reservation.checkIn())
                        + " a " + TextFormat.date(reservation.checkOut()) + ".\\n");
    }}
}}
""")
pattern.write("src/main/java/hotel/notifications/FinanceNotice.java", """package hotel.notifications;

import hotel.documents.TextFormat;
import hotel.messages.EmailSender;
import hotel.reservations.Reservation;
import hotel.reservations.ReservationListener;
import hotel.rooms.Room;

/** Finance hears about every payment and every refund (a cancellation with nothing to refund is not news). */
public final class FinanceNotice implements ReservationListener {

    static final String FINANCE_EMAIL = "financeiro@pousadamarealta.com.br";

    private final EmailSender emailSender;

    public FinanceNotice(EmailSender emailSender) {
        this.emailSender = emailSender;
    }

    @Override
    public void confirmed(Reservation reservation, Room room) {
        emailSender.send(FINANCE_EMAIL, "Pagamento recebido - reserva " + reservation.id(),
                "Reserva " + reservation.id() + " (" + reservation.guest().name() + "): pagamento de "
                        + TextFormat.money(reservation.amountPaid()) + ".\\n");
    }

    @Override
    public void cancelled(Reservation reservation, Room room) {
        if (reservation.refund().signum() > 0) {
            emailSender.send(FINANCE_EMAIL, "Reembolso - reserva " + reservation.id(),
                    "Devolver " + TextFormat.money(reservation.refund()) + " para "
                            + reservation.guest().name() + ".\\n");
        }
    }
}
""")
pattern.replace(SERVICE, "import hotel.documents.ReservationReceipt;\nimport hotel.messages.EmailSender;\n",
                "import hotel.documents.ReservationReceipt;\n")
pattern.replace(SERVICE, "import java.math.BigDecimal;\n", "import java.math.BigDecimal;\nimport java.util.List;\n")
pattern.replace(SERVICE, """    static final String HOTEL_NAME = "Pousada Maré Alta";

    private final Rooms rooms;
    private final ReservationStore store;
    private final PriceCalculator priceCalculator;
    private final EmailSender emailSender;

    public ReservationService(
            Rooms rooms, ReservationStore store, PriceCalculator priceCalculator, EmailSender emailSender) {
        this.rooms = rooms;
        this.store = store;
        this.priceCalculator = priceCalculator;
        this.emailSender = emailSender;
    }""", """    private final Rooms rooms;
    private final ReservationStore store;
    private final PriceCalculator priceCalculator;
    private final List<ReservationListener> listeners;

    public ReservationService(Rooms rooms, ReservationStore store, PriceCalculator priceCalculator,
            List<ReservationListener> listeners) {
        this.rooms = rooms;
        this.store = store;
        this.priceCalculator = priceCalculator;
        this.listeners = List.copyOf(listeners);
    }""")
pattern.replace(SERVICE, """    /** The guest paid: the reservation is confirmed and the guest gets the receipt by e-mail. */""",
                """    /** The guest paid: the reservation is confirmed, and the listeners hear about it. */""")
pattern.replace(SERVICE, """        reservation.confirmPayment();
        ReservationReceipt receipt = new ReservationReceipt(reservation, roomOf(reservation));
        emailSender.send(
                reservation.guest().email(),
                "Reserva confirmada - " + HOTEL_NAME,
                confirmationEmailBody(reservation, receipt));
        return reservation;""", """        reservation.confirmPayment();
        Room room = roomOf(reservation);
        listeners.forEach(listener -> listener.confirmed(reservation, room));
        return reservation;""")
pattern.replace(SERVICE, """            throw notAllowed(reservation, "cancel");
        }
        BigDecimal refund""", """            throw notAllowed(reservation, "cancel");
        }
        boolean wasConfirmed = reservation.status() == ReservationStatus.CONFIRMED;
        BigDecimal refund""")
pattern.replace(SERVICE, """        reservation.cancel(refund);
        return reservation;""", """        reservation.cancel(refund);
        if (wasConfirmed) {
            Room room = roomOf(reservation);
            listeners.forEach(listener -> listener.cancelled(reservation, room));
        }
        return reservation;""")
pattern.replace(SERVICE, """    private static String confirmationEmailBody(Reservation reservation, ReservationReceipt receipt) {
        return "Olá, " + reservation.guest().name() + "!\\n\\n"
                + "Sua reserva na " + HOTEL_NAME + " está confirmada. Estes são os dados dela:\\n\\n"
                + receipt.text() + "\\n"
                + "A entrada é a partir das 14h, e a saída, até as 12h.\\n"
                + "Até breve!\\n";
    }

""", "")
pattern.replace(HOTEL, "import hotel.messages.SentMessages;\n",
                "import hotel.messages.SentMessages;\nimport hotel.notifications.FinanceNotice;\n"
                "import hotel.notifications.GuestConfirmationEmail;\nimport hotel.notifications.HousekeepingNotice;\n")
pattern.replace(HOTEL, "import io.javalin.Javalin;\n", "import io.javalin.Javalin;\nimport java.util.List;\n")
pattern.replace(HOTEL, """        ReservationService reservations =
                new ReservationService(rooms, store, new PriceCalculator(), new EmailSender(sentMessages));""",
                """        EmailSender emailSender = new EmailSender(sentMessages);
        ReservationService reservations = new ReservationService(rooms, store, new PriceCalculator(), List.of(
                new GuestConfirmationEmail(emailSender),
                new HousekeepingNotice(emailSender),
                new FinanceNotice(emailSender)));""")
pattern.replace(SERVICE_TEST, "import hotel.messages.SentMessages;\n",
                "import hotel.messages.SentMessages;\nimport hotel.notifications.GuestConfirmationEmail;\n")
pattern.replace(SERVICE_TEST, """    private final ReservationService service = new ReservationService(
            new Rooms(), new ReservationStore(), new PriceCalculator(), new EmailSender(sentMessages));""",
                """    private final ReservationService service = new ReservationService(new Rooms(), new ReservationStore(),
            new PriceCalculator(), List.of(new GuestConfirmationEmail(new EmailSender(sentMessages))));""")
pattern.replace(REPORTS_TEST, "import hotel.messages.EmailSender;\nimport hotel.messages.SentMessages;\n", "")
pattern.replace(REPORTS_TEST, "import java.time.YearMonth;\n", "import java.time.YearMonth;\nimport java.util.List;\n")
pattern.replace(REPORTS_TEST, """    private final ReservationService service =
            new ReservationService(rooms, store, new PriceCalculator(), new EmailSender(new SentMessages()));""",
                """    private final ReservationService service = new ReservationService(rooms, store, new PriceCalculator(), List.of());""")
pattern.replace(HTTP_TEST, "        assertEquals(1, messages.size());", "        assertEquals(3, messages.size());")
pattern.write("src/test/java/hotel/notifications/TeamNoticesTest.java", """package hotel.notifications;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import hotel.messages.EmailSender;
import hotel.messages.Message;
import hotel.messages.SentMessages;
import hotel.reservations.Guest;
import hotel.reservations.NewReservation;
import hotel.reservations.PriceCalculator;
import hotel.reservations.RatePlan;
import hotel.reservations.Reservation;
import hotel.reservations.ReservationService;
import hotel.reservations.ReservationStore;
import hotel.rooms.Rooms;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class TeamNoticesTest {

    private final SentMessages sentMessages = new SentMessages();
    private final EmailSender emailSender = new EmailSender(sentMessages);
    private final ReservationService service = new ReservationService(new Rooms(), new ReservationStore(),
            new PriceCalculator(), List.of(new HousekeepingNotice(emailSender), new FinanceNotice(emailSender)));

    private Reservation book(int room, RatePlan plan) {
        Guest guest = new Guest("Ana Souza", "ana@example.com", "123.456.789-09");
        return service.create(new NewReservation(
                room, guest, LocalDate.of(2027, 3, 10), LocalDate.of(2027, 3, 12), 2, plan));
    }

    private List<String> subjects() {
        return sentMessages.all().stream().map(Message::subject).toList();
    }

    @Test
    void aConfirmationWarnsHousekeepingAndFinance() {
        Reservation reservation = book(201, RatePlan.STANDARD);
        service.confirm(reservation.id());

        assertEquals(List.of("Reserva confirmada - quarto 201", "Pagamento recebido - reserva 1"), subjects());
        assertTrue(sentMessages.all().get(1).body().contains("R$ 779,80"));
    }

    @Test
    void aRefundWarnsFinanceAndNothingToRefundDoesNot() {
        Reservation standard = book(201, RatePlan.STANDARD);
        Reservation nonRefundable = book(202, RatePlan.NON_REFUNDABLE);
        service.confirm(standard.id());
        service.confirm(nonRefundable.id());
        service.cancel(standard.id());
        service.cancel(nonRefundable.id());

        assertEquals(List.of("Reserva confirmada - quarto 201", "Pagamento recebido - reserva 1",
                "Reserva confirmada - quarto 202", "Pagamento recebido - reserva 2",
                "Reserva cancelada - quarto 201", "Reembolso - reserva 1",
                "Reserva cancelada - quarto 202"), subjects());
    }

    @Test
    void cancellingAPendingReservationWarnsNobody() {
        service.cancel(book(201, RatePlan.STANDARD).id());

        assertEquals(List.of(), subjects());
    }
}
""")
pattern.build_and_save()

# ---------------------------------------------------------------- naive: the service sends everything itself
naive = Solution("03-naive")
naive.replace(SERVICE, "import hotel.documents.ReservationReceipt;\n",
              "import hotel.documents.ReservationReceipt;\nimport hotel.documents.TextFormat;\n")
naive.replace(SERVICE, """    static final String HOTEL_NAME = "Pousada Maré Alta";
""", """    static final String HOTEL_NAME = "Pousada Maré Alta";
    static final String HOUSEKEEPING_EMAIL = "governanca@pousadamarealta.com.br";
    static final String FINANCE_EMAIL = "financeiro@pousadamarealta.com.br";
""")
naive.replace(SERVICE, """                confirmationEmailBody(reservation, receipt));
        return reservation;""", f"""                confirmationEmailBody(reservation, receipt));
        Room room = roomOf(reservation);
        emailSender.send(HOUSEKEEPING_EMAIL, "Reserva confirmada - quarto " + room.number(),
                {HOUSEKEEPING_BODY_CONFIRMED});
        emailSender.send(FINANCE_EMAIL, "Pagamento recebido - reserva " + reservation.id(),
                "Reserva " + reservation.id() + " (" + reservation.guest().name() + "): pagamento de "
                        + TextFormat.money(reservation.amountPaid()) + ".\\n");
        return reservation;""")
naive.replace(SERVICE, """            throw notAllowed(reservation, "cancel");
        }
        BigDecimal refund""", """            throw notAllowed(reservation, "cancel");
        }
        boolean wasConfirmed = reservation.status() == ReservationStatus.CONFIRMED;
        BigDecimal refund""")
naive.replace(SERVICE, """        reservation.cancel(refund);
        return reservation;""", """        reservation.cancel(refund);
        if (wasConfirmed) {
            int roomNumber = reservation.roomNumber();
            emailSender.send(HOUSEKEEPING_EMAIL, "Reserva cancelada - quarto " + roomNumber,
                    "O quarto " + roomNumber + " ficou livre de " + TextFormat.date(reservation.checkIn())
                            + " a " + TextFormat.date(reservation.checkOut()) + ".\\n");
            if (refund.signum() > 0) {
                emailSender.send(FINANCE_EMAIL, "Reembolso - reserva " + reservation.id(),
                        "Devolver " + TextFormat.money(refund) + " para " + reservation.guest().name() + ".\\n");
            }
        }
        return reservation;""")
naive.replace(SERVICE_TEST, "        assertEquals(1, sent.size());", "        assertEquals(3, sent.size());")
naive.replace(SERVICE_TEST, "        assertEquals(1, sentMessages.all().size());",
              "        assertEquals(3, sentMessages.all().size());")
naive.replace(HTTP_TEST, "        assertEquals(1, messages.size());", "        assertEquals(3, messages.size());")
naive.build_and_save()
