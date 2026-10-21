package com.pousadamaretalta;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/** Texto e título do documento emitido para uma reserva paga. */
public interface DocumentTemplate {
    String title();

    String render(ReservationDocument d);

    DocumentTemplate COMPROVANTE = new DocumentTemplate() {
        public String title() { return "Comprovante de reserva"; }

        public String render(ReservationDocument d) {
            List<String> l = new ArrayList<>();
            l.add(title() + " nº " + d.reservationId());
            l.add("Hóspede: " + d.guestName() + " (CPF " + d.identity().cpf() + ")");
            ptBody(l, d);
            return String.join("\n", l);
        }
    };

    DocumentTemplate FATURA = new DocumentTemplate() {
        public String title() { return "Fatura de hospedagem"; }

        public String render(ReservationDocument d) {
            List<String> l = new ArrayList<>();
            l.add(title() + " nº " + d.reservationId());
            l.add("Empresa: " + d.identity().companyName() + " (CNPJ " + d.identity().cnpj() + ")");
            l.add("Hóspede: " + d.guestName());
            ptBody(l, d);
            l.add("ISS retido (5%): " + Money.brl(Money.round(d.total().multiply(new java.math.BigDecimal("0.05")))));
            return String.join("\n", l);
        }
    };

    DocumentTemplate BOOKING_RECEIPT = new DocumentTemplate() {
        public String title() { return "Booking receipt"; }

        public String render(ReservationDocument d) {
            DateTimeFormatter iso = DateTimeFormatter.ISO_LOCAL_DATE;
            List<String> l = new ArrayList<>();
            l.add(title() + " no. " + d.reservationId());
            l.add("Guest: " + d.guestName() + " (passport " + d.identity().passport() + ", "
                    + d.identity().country() + ")");
            l.add("Room: " + d.roomNumber() + " (" + d.roomType() + ")");
            l.add("Check-in: " + iso.format(d.checkIn()) + " | Check-out: " + iso.format(d.checkOut())
                    + " | " + d.nights() + (d.nights() == 1 ? " night" : " nights"));
            l.add("Nights: " + Money.brlEn(d.subtotal()));
            if (d.discount().signum() > 0) l.add("Discount: " + Money.brlEn(d.discount()));
            l.add("Total: " + Money.brlEn(d.total()));
            return String.join("\n", l);
        }
    };

    /** Linhas em português comuns ao comprovante e à fatura: quarto, datas, diárias, desconto, total. */
    private static void ptBody(List<String> l, ReservationDocument d) {
        DateTimeFormatter br = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        l.add("Quarto: " + d.roomNumber() + " (" + d.roomType() + ")");
        l.add("Entrada: " + br.format(d.checkIn()) + " | Saída: " + br.format(d.checkOut())
                + " | " + d.nights() + (d.nights() == 1 ? " diária" : " diárias"));
        l.add("Diárias: " + Money.brl(d.subtotal()));
        if (d.discount().signum() > 0) l.add("Desconto: " + Money.brl(d.discount()));
        l.add("Total: " + Money.brl(d.total()));
    }
}
