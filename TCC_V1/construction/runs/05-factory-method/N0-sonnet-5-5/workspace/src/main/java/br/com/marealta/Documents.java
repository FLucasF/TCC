package br.com.marealta;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Documento emitido para a reserva paga. O tipo depende de como o hóspede se identificou;
 * para um novo tipo (ex.: agência), basta um novo Identification e um novo case no switch,
 * que o compilador obriga a tratar.
 */
final class Documents {
    private Documents() {}

    record Document(String title, String text) {}

    private static final DateTimeFormatter BR = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    static Document render(Reservation r, Room room) {
        return switch (r.identification()) {
            case Identification.Cpf id -> receipt(r, room, id);
            case Identification.Company id -> invoice(r, room, id);
            case Identification.Passport id -> bookingReceipt(r, room, id);
        };
    }

    private static Document receipt(Reservation r, Room room, Identification.Cpf id) {
        String title = "Comprovante de reserva";
        List<String> lines = portugueseLines(r, room, title,
                "Hóspede: " + r.guestName() + " (CPF " + id.cpf() + ")");
        return new Document(title, String.join("\n", lines));
    }

    private static Document invoice(Reservation r, Room room, Identification.Company id) {
        String title = "Fatura de hospedagem";
        List<String> lines = portugueseLines(r, room, title,
                "Empresa: " + id.companyName() + " (CNPJ " + id.cnpj() + ")",
                "Hóspede: " + r.guestName());
        lines.add("ISS retido (5%): " + Money.brl(Pricing.iss(r.total())));
        return new Document(title, String.join("\n", lines));
    }

    private static List<String> portugueseLines(Reservation r, Room room, String title, String... guestLines) {
        List<String> lines = new ArrayList<>();
        lines.add(title + " nº " + r.id());
        lines.addAll(List.of(guestLines));
        lines.add("Quarto: " + room.number() + " (" + room.type() + ")");
        lines.add("Entrada: " + BR.format(r.checkIn()) + " | Saída: " + BR.format(r.checkOut())
                + " | " + r.nights() + (r.nights() == 1 ? " diária" : " diárias"));
        lines.add("Diárias: " + Money.brl(r.subtotal()));
        if (r.discount().signum() > 0) lines.add("Desconto: " + Money.brl(r.discount()));
        lines.add("Total: " + Money.brl(r.total()));
        return lines;
    }

    private static Document bookingReceipt(Reservation r, Room room, Identification.Passport id) {
        String title = "Booking receipt";
        List<String> lines = new ArrayList<>();
        lines.add(title + " no. " + r.id());
        lines.add("Guest: " + r.guestName() + " (passport " + id.passport() + ", " + id.country() + ")");
        lines.add("Room: " + room.number() + " (" + room.type() + ")");
        lines.add("Check-in: " + r.checkIn() + " | Check-out: " + r.checkOut()
                + " | " + r.nights() + (r.nights() == 1 ? " night" : " nights"));
        lines.add("Nights: " + Money.en(r.subtotal()));
        if (r.discount().signum() > 0) lines.add("Discount: " + Money.en(r.discount()));
        lines.add("Total: " + Money.en(r.total()));
        return new Document(title, String.join("\n", lines));
    }

    static String confirmationSubject() {
        return "Reserva confirmada - Pousada Maré Alta";
    }

    /** E-mail sempre em português; só o documento no meio muda. */
    static String confirmationBody(Reservation r, Document doc) {
        return "Olá, " + r.guestName() + "!\n\n"
                + "Sua reserva na Pousada Maré Alta está confirmada. Estes são os dados dela:\n\n"
                + doc.text() + "\n\n"
                + "A entrada é a partir das 14h, e a saída, até as 12h.\n"
                + "Até breve!";
    }

}
