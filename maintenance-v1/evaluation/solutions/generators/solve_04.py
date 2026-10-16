"""Scenario 04 (negative control): the contained solution (new strings) and the over-engineered one (a confirmation
channel interface "for when WhatsApp comes")."""

from kit import HOTEL, REPORTS_TEST, SERVICE, SERVICE_TEST, Solution

OLD_SUBJECT = '"Reserva confirmada - " + HOTEL_NAME'
NEW_SUBJECT = '"Sua reserva na " + HOTEL_NAME + " está confirmada!"'
OLD_LINE = '                + "A entrada é a partir das 14h, e a saída, até as 12h.\\n"\n'
NEW_LINES = ('                + "Entrada a partir das 14h e saída até as 12h.\\n"\n'
             '                + "Café da manhã das 7h às 10h, incluído na diária.\\n"\n'
             '                + "Qualquer dúvida, fale com a recepção: (79) 3200-0000.\\n"\n')


def update_service_test(solution: Solution) -> None:
    solution.replace(SERVICE_TEST, 'assertEquals("Reserva confirmada - Pousada Maré Alta", sent.get(0).subject());',
                     'assertEquals("Sua reserva na Pousada Maré Alta está confirmada!", sent.get(0).subject());')


contained = Solution("04-contained")
contained.replace(SERVICE, OLD_SUBJECT, NEW_SUBJECT)
contained.replace(SERVICE, OLD_LINE, NEW_LINES)
update_service_test(contained)
contained.build_and_save()


over = Solution("04-overengineered")
over.write("src/main/java/hotel/messages/ConfirmationChannel.java", """package hotel.messages;

import hotel.documents.ReservationReceipt;

/** A way of telling the guest that the reservation is confirmed (e-mail today; WhatsApp some day). */
public interface ConfirmationChannel {

    void sendConfirmation(String guestName, String guestEmail, ReservationReceipt receipt);
}
""")
over.write("src/main/java/hotel/messages/EmailConfirmationChannel.java", """package hotel.messages;

import hotel.documents.ReservationReceipt;

/** The confirmation by e-mail. */
public final class EmailConfirmationChannel implements ConfirmationChannel {

    static final String HOTEL_NAME = "Pousada Maré Alta";

    private final EmailSender emailSender;

    public EmailConfirmationChannel(EmailSender emailSender) {
        this.emailSender = emailSender;
    }

    @Override
    public void sendConfirmation(String guestName, String guestEmail, ReservationReceipt receipt) {
        emailSender.send(guestEmail, "Sua reserva na " + HOTEL_NAME + " está confirmada!", body(guestName, receipt));
    }

    private static String body(String guestName, ReservationReceipt receipt) {
        return "Olá, " + guestName + "!\\n\\n"
                + "Sua reserva na " + HOTEL_NAME + " está confirmada. Estes são os dados dela:\\n\\n"
                + receipt.text() + "\\n"
                + "Entrada a partir das 14h e saída até as 12h.\\n"
                + "Café da manhã das 7h às 10h, incluído na diária.\\n"
                + "Qualquer dúvida, fale com a recepção: (79) 3200-0000.\\n"
                + "Até breve!\\n";
    }
}
""")
over.replace(SERVICE, "import hotel.messages.EmailSender;\n", "import hotel.messages.ConfirmationChannel;\n")
over.replace(SERVICE, """    static final String HOTEL_NAME = "Pousada Maré Alta";

""", "")
over.replace(SERVICE, """    private final EmailSender emailSender;

    public ReservationService(
            Rooms rooms, ReservationStore store, PriceCalculator priceCalculator, EmailSender emailSender) {
        this.rooms = rooms;
        this.store = store;
        this.priceCalculator = priceCalculator;
        this.emailSender = emailSender;
    }""", """    private final ConfirmationChannel confirmationChannel;

    public ReservationService(Rooms rooms, ReservationStore store, PriceCalculator priceCalculator,
            ConfirmationChannel confirmationChannel) {
        this.rooms = rooms;
        this.store = store;
        this.priceCalculator = priceCalculator;
        this.confirmationChannel = confirmationChannel;
    }""")
over.replace(SERVICE, """        emailSender.send(
                reservation.guest().email(),
                "Reserva confirmada - " + HOTEL_NAME,
                confirmationEmailBody(reservation, receipt));""",
             """        confirmationChannel.sendConfirmation(reservation.guest().name(), reservation.guest().email(), receipt);""")
over.replace(SERVICE, """    private static String confirmationEmailBody(Reservation reservation, ReservationReceipt receipt) {
        return "Olá, " + reservation.guest().name() + "!\\n\\n"
                + "Sua reserva na " + HOTEL_NAME + " está confirmada. Estes são os dados dela:\\n\\n"
                + receipt.text() + "\\n"
                + "A entrada é a partir das 14h, e a saída, até as 12h.\\n"
                + "Até breve!\\n";
    }

""", "")
over.replace(HOTEL, "import hotel.messages.EmailSender;\n",
             "import hotel.messages.EmailConfirmationChannel;\nimport hotel.messages.EmailSender;\n")
over.replace(HOTEL, "new ReservationService(rooms, store, new PriceCalculator(), new EmailSender(sentMessages));",
             "new ReservationService(rooms, store, new PriceCalculator(),\n"
             "                        new EmailConfirmationChannel(new EmailSender(sentMessages)));")
over.replace(SERVICE_TEST, "import hotel.messages.EmailSender;\n",
             "import hotel.messages.EmailConfirmationChannel;\nimport hotel.messages.EmailSender;\n")
over.replace(SERVICE_TEST, "new PriceCalculator(), new EmailSender(sentMessages));",
             "new PriceCalculator(),\n            new EmailConfirmationChannel(new EmailSender(sentMessages)));")
over.replace(REPORTS_TEST, "import hotel.messages.EmailSender;\n",
             "import hotel.messages.EmailConfirmationChannel;\nimport hotel.messages.EmailSender;\n")
over.replace(REPORTS_TEST, "new ReservationService(rooms, store, new PriceCalculator(), new EmailSender(new SentMessages()));",
             "new ReservationService(rooms, store, new PriceCalculator(),\n"
             "                    new EmailConfirmationChannel(new EmailSender(new SentMessages())));")
update_service_test(over)
over.build_and_save()
