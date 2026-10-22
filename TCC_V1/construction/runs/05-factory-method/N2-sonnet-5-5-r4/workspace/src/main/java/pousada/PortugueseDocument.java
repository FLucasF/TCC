package pousada;

import java.time.format.DateTimeFormatter;

/** Parte em português, comum aos documentos que saem em português. */
abstract class PortugueseDocument extends Document {
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @Override
    protected String titleLine(Reservation r) {
        return title() + " nº " + r.id();
    }

    @Override
    protected String roomLine(Reservation r) {
        return "Quarto: " + r.room().number() + " (" + r.room().type() + ")";
    }

    @Override
    protected String periodLine(Reservation r) {
        int nights = r.pricing().nights();
        return "Entrada: " + r.checkIn().format(DATE) + " | Saída: " + r.checkOut().format(DATE) + " | "
                + nights + (nights == 1 ? " diária" : " diárias");
    }

    @Override
    protected String subtotalLine(Reservation r) {
        return "Diárias: " + Money.brl(r.pricing().subtotal());
    }

    @Override
    protected String discountLine(Reservation r) {
        return "Desconto: " + Money.brl(r.pricing().discount());
    }

    @Override
    protected String totalLine(Reservation r) {
        return "Total: " + Money.brl(r.pricing().total());
    }
}
