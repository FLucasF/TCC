package br.com.marealta.domain;

import java.time.format.DateTimeFormatter;

/** Parte em português comum ao comprovante e à fatura. */
abstract class PortugueseDocument extends ReservationDocument {
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    protected PortugueseDocument(Reservation reservation) {
        super(reservation);
    }

    @Override
    protected String heading() {
        return title() + " nº " + reservation.id();
    }

    @Override
    protected String roomLine() {
        return "Quarto: " + reservation.room().number() + " (" + reservation.room().type() + ")";
    }

    @Override
    protected String periodLine() {
        int nights = reservation.nights();
        return "Entrada: " + DATE.format(reservation.checkIn()) + " | Saída: " + DATE.format(reservation.checkOut())
                + " | " + nights + (nights == 1 ? " diária" : " diárias");
    }

    @Override
    protected String subtotalLine() {
        return "Diárias: " + Money.brl(reservation.subtotal());
    }

    @Override
    protected String discountLine() {
        return "Desconto: " + Money.brl(reservation.discount());
    }

    @Override
    protected String totalLine() {
        return "Total: " + Money.brl(reservation.total());
    }
}
