package pousada;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/** Builds the document that matches the guest's identification. */
public final class Documents {
    private static final DateTimeFormatter PT_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final BigDecimal ISS_RATE = new BigDecimal("0.05");

    private Documents() {}

    public static BookingDocument issue(Reservation r) {
        Room room = r.room();
        String title;
        String text;
        switch (r.identity()) {
            case GuestIdentity.Individual i -> {
                title = "Comprovante de reserva";
                text = portuguese(r, room, title,
                        "Hóspede: " + r.guestName() + " (CPF " + i.cpf() + ")", false);
            }
            case GuestIdentity.Company c -> {
                title = "Fatura de hospedagem";
                text = portuguese(r, room, title,
                        "Empresa: " + c.companyName() + " (CNPJ " + c.cnpj() + ")\nHóspede: " + r.guestName(), true);
            }
            case GuestIdentity.Foreigner f -> {
                title = "Booking receipt";
                text = english(r, room, title, f);
            }
        }
        return new BookingDocument(title, r.id(), r.guestName(), r.identity(), room.number(), room.type(),
                r.checkIn(), r.checkOut(), r.nights(), r.subtotal(), r.discount(), r.total(), text);
    }

    private static String portuguese(Reservation r, Room room, String title, String guestLines, boolean iss) {
        List<String> lines = new ArrayList<>();
        lines.add(title + " nº " + r.id());
        lines.add(guestLines);
        lines.add("Quarto: " + room.number() + " (" + room.type() + ")");
        lines.add("Entrada: " + PT_DATE.format(r.checkIn()) + " | Saída: " + PT_DATE.format(r.checkOut())
                + " | " + r.nights() + (r.nights() == 1 ? " diária" : " diárias"));
        lines.add("Diárias: " + Money.brl(r.subtotal()));
        if (r.discount().signum() > 0) lines.add("Desconto: " + Money.brl(r.discount()));
        lines.add("Total: " + Money.brl(r.total()));
        if (iss) lines.add("ISS retido (5%): " + Money.brl(Money.round(r.total().multiply(ISS_RATE))));
        return String.join("\n", lines);
    }

    private static String english(Reservation r, Room room, String title, GuestIdentity.Foreigner f) {
        List<String> lines = new ArrayList<>();
        lines.add(title + " no. " + r.id());
        lines.add("Guest: " + r.guestName() + " (passport " + f.passport() + ", " + f.country() + ")");
        lines.add("Room: " + room.number() + " (" + room.type() + ")");
        lines.add("Check-in: " + r.checkIn() + " | Check-out: " + r.checkOut()
                + " | " + r.nights() + (r.nights() == 1 ? " night" : " nights"));
        lines.add("Nights: " + Money.brlEn(r.subtotal()));
        if (r.discount().signum() > 0) lines.add("Discount: " + Money.brlEn(r.discount()));
        lines.add("Total: " + Money.brlEn(r.total()));
        return String.join("\n", lines);
    }

    public static String confirmationSubject() {
        return "Reserva confirmada - Pousada Maré Alta";
    }

    /** The confirmation e-mail is always in Portuguese; only the embedded document varies. */
    public static String confirmationBody(Reservation r, BookingDocument doc) {
        return "Olá, " + r.guestName() + "!\n\n"
                + "Sua reserva na Pousada Maré Alta está confirmada. Estes são os dados dela:\n\n"
                + doc.text() + "\n\n"
                + "A entrada é a partir das 14h, e a saída, até as 12h.\n"
                + "Até breve!";
    }
}
