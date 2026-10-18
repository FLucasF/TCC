package hotel.documents;

/** The receipt for a guest with CPF, in Portuguese. The invoice builds on it. */
class ReceiptLayout implements DocumentLayout {

    @Override
    public String title() {
        return "Comprovante de reserva";
    }

    @Override
    public String text(ReservationDocument document) {
        StringBuilder text = new StringBuilder();
        text.append(document.title()).append(" nº ").append(document.reservationId()).append('\n');
        text.append(identification(document));
        text.append("Quarto: ").append(document.roomNumber()).append(" (").append(document.roomType()).append(")\n");
        text.append("Entrada: ").append(TextFormat.date(document.checkIn()))
                .append(" | Saída: ").append(TextFormat.date(document.checkOut()))
                .append(" | ").append(document.nights()).append(document.nights() == 1 ? " diária" : " diárias")
                .append('\n');
        text.append("Diárias: ").append(TextFormat.money(document.subtotal())).append('\n');
        if (document.discount().signum() > 0) {
            text.append("Desconto: ").append(TextFormat.money(document.discount())).append('\n');
        }
        text.append("Total: ").append(TextFormat.money(document.total())).append('\n');
        text.append(closing(document));
        return text.toString();
    }

    /** The lines that say who the document is for. */
    String identification(ReservationDocument document) {
        return "Hóspede: " + document.guestName() + " (CPF " + document.guestCpf() + ")\n";
    }

    /** The lines after the total. */
    String closing(ReservationDocument document) {
        return "";
    }
}
