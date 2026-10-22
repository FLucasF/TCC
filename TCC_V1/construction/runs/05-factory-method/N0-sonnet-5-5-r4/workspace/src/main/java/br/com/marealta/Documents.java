package br.com.marealta;

import br.com.marealta.Model.Identification;
import br.com.marealta.Model.Reservation;
import br.com.marealta.Model.Room;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/** Documentos emitidos para a reserva; o tipo depende de como o hóspede se identificou. */
public final class Documents {
    private static final DateTimeFormatter PT_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private Documents() {}

    public record Document(String title, String text) {}

    public static Document render(Reservation r, Room room) {
        // O switch sobre a interface selada obriga a tratar todo novo tipo de identificação.
        return switch (r.identification()) {
            case Identification.Cpf id -> receipt(r, room, id);
            case Identification.Company id -> invoice(r, room, id);
            case Identification.Passport id -> bookingReceipt(r, room, id);
        };
    }

    private static Document receipt(Reservation r, Room room, Identification.Cpf id) {
        String title = "Comprovante de reserva";
        List<String> lines = new ArrayList<>();
        lines.add(title + " nº " + r.id());
        lines.add("Hóspede: " + r.guestName() + " (CPF " + id.cpf() + ")");
        addPtBody(lines, r, room);
        return new Document(title, String.join("\n", lines));
    }

    private static Document invoice(Reservation r, Room room, Identification.Company id) {
        String title = "Fatura de hospedagem";
        List<String> lines = new ArrayList<>();
        lines.add(title + " nº " + r.id());
        lines.add("Empresa: " + id.name() + " (CNPJ " + id.cnpj() + ")");
        lines.add("Hóspede: " + r.guestName());
        addPtBody(lines, r, room);
        lines.add("ISS retido (5%): R$ " + Money.pt(Pricing.withheldIss(r.total())));
        return new Document(title, String.join("\n", lines));
    }

    private static Document bookingReceipt(Reservation r, Room room, Identification.Passport id) {
        String title = "Booking receipt";
        List<String> lines = new ArrayList<>();
        lines.add(title + " no. " + r.id());
        lines.add("Guest: " + r.guestName() + " (passport " + id.number() + ", " + id.country() + ")");
        lines.add("Room: " + room.number() + " (" + room.type() + ")");
        lines.add("Check-in: " + r.checkIn() + " | Check-out: " + r.checkOut() + " | "
                + r.nights() + (r.nights() == 1 ? " night" : " nights"));
        lines.add("Nights: BRL " + Money.en(r.subtotal()));
        if (r.discount().signum() > 0) lines.add("Discount: BRL " + Money.en(r.discount()));
        lines.add("Total: BRL " + Money.en(r.total()));
        return new Document(title, String.join("\n", lines));
    }

    private static void addPtBody(List<String> lines, Reservation r, Room room) {
        lines.add("Quarto: " + room.number() + " (" + room.type() + ")");
        lines.add("Entrada: " + date(r.checkIn()) + " | Saída: " + date(r.checkOut()) + " | "
                + r.nights() + (r.nights() == 1 ? " diária" : " diárias"));
        lines.add("Diárias: R$ " + Money.pt(r.subtotal()));
        BigDecimal discount = r.discount();
        if (discount.signum() > 0) lines.add("Desconto: R$ " + Money.pt(discount));
        lines.add("Total: R$ " + Money.pt(r.total()));
    }

    private static String date(LocalDate d) {
        return PT_DATE.format(d);
    }

    public static String confirmationEmailBody(Reservation r, Document doc) {
        return "Olá, " + r.guestName() + "!\n\n"
                + "Sua reserva na Pousada Maré Alta está confirmada. Estes são os dados dela:\n\n"
                + doc.text() + "\n\n"
                + "A entrada é a partir das 14h, e a saída, até as 12h.\n"
                + "Até breve!";
    }

    public static final String CONFIRMATION_SUBJECT = "Reserva confirmada - Pousada Maré Alta";
}
