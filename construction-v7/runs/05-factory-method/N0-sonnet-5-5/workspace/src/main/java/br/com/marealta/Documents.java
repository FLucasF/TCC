package br.com.marealta;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Builds the document that fits the guest's identification. The switch is exhaustive, so
 * a new identification type (e.g. travel agency) will not compile until it gets a document.
 */
final class Documents {
    private static final DateTimeFormatter PT_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final BigDecimal ISS_RATE = new BigDecimal("0.05");

    private Documents() {}

    static Document render(Reservation r) {
        return switch (r.identification) {
            case Identification.Cpf cpf -> receipt(r, cpf);
            case Identification.Cnpj cnpj -> invoice(r, cnpj);
            case Identification.Passport p -> bookingReceipt(r, p);
        };
    }

    private static Document receipt(Reservation r, Identification.Cpf cpf) {
        String title = "Comprovante de reserva";
        List<String> lines = new ArrayList<>();
        lines.add(title + " nº " + r.id);
        lines.add("Hóspede: " + r.guestName + " (CPF " + cpf.cpf() + ")");
        addPortugueseBody(lines, r);
        return new Document(title, r, String.join("\n", lines));
    }

    private static Document invoice(Reservation r, Identification.Cnpj cnpj) {
        String title = "Fatura de hospedagem";
        List<String> lines = new ArrayList<>();
        lines.add(title + " nº " + r.id);
        lines.add("Empresa: " + cnpj.companyName() + " (CNPJ " + cnpj.cnpj() + ")");
        lines.add("Hóspede: " + r.guestName);
        addPortugueseBody(lines, r);
        lines.add("ISS retido (5%): " + Money.brl(Money.round(r.quote.total().multiply(ISS_RATE))));
        return new Document(title, r, String.join("\n", lines));
    }

    private static void addPortugueseBody(List<String> lines, Reservation r) {
        Pricing.Quote q = r.quote;
        lines.add("Quarto: " + r.room.number() + " (" + r.room.type() + ")");
        lines.add("Entrada: " + PT_DATE.format(r.checkIn) + " | Saída: " + PT_DATE.format(r.checkOut)
                + " | " + q.nights() + (q.nights() == 1 ? " diária" : " diárias"));
        lines.add("Diárias: " + Money.brl(q.subtotal()));
        if (q.discount().signum() > 0) {
            lines.add("Desconto: " + Money.brl(q.discount()));
        }
        lines.add("Total: " + Money.brl(q.total()));
    }

    private static Document bookingReceipt(Reservation r, Identification.Passport p) {
        String title = "Booking receipt";
        Pricing.Quote q = r.quote;
        List<String> lines = new ArrayList<>();
        lines.add(title + " no. " + r.id);
        lines.add("Guest: " + r.guestName + " (passport " + p.passport() + ", " + p.country() + ")");
        lines.add("Room: " + r.room.number() + " (" + r.room.type() + ")");
        lines.add("Check-in: " + r.checkIn + " | Check-out: " + r.checkOut
                + " | " + q.nights() + (q.nights() == 1 ? " night" : " nights"));
        lines.add("Nights: " + Money.brlEn(q.subtotal()));
        if (q.discount().signum() > 0) {
            lines.add("Discount: " + Money.brlEn(q.discount()));
        }
        lines.add("Total: " + Money.brlEn(q.total()));
        return new Document(title, r, String.join("\n", lines));
    }
}
