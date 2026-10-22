package pousada.domain;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Comprovante de reserva paga. */
public record Document(String title, long reservationId, String guestName, String guestCpf,
                       int roomNumber, RoomType roomType, LocalDate checkIn, LocalDate checkOut,
                       int nights, BigDecimal subtotal, BigDecimal discount, BigDecimal total) {

    public static final String TITLE = "Comprovante de reserva";

    public static Document of(Reservation r) {
        if (!r.status().isPaid()) {
            throw new DomainException(DomainException.Code.OPERATION_NOT_ALLOWED,
                    "Reserva " + r.id() + " não está paga e não tem comprovante");
        }
        Quote q = r.quote();
        return new Document(TITLE, r.id(), r.guest().name(), r.guest().cpf(), r.room().number(),
                r.room().type(), r.checkIn(), r.checkOut(), q.nights(), q.subtotal(), q.discount(), q.total());
    }

    public String render() {
        StringBuilder text = new StringBuilder()
                .append(TITLE).append(" nº ").append(reservationId).append('\n')
                .append("Hóspede: ").append(guestName).append(" (CPF ").append(guestCpf).append(")\n")
                .append("Quarto: ").append(roomNumber).append(" (").append(roomType).append(")\n")
                .append("Entrada: ").append(Formats.date(checkIn))
                .append(" | Saída: ").append(Formats.date(checkOut))
                .append(" | ").append(nights).append(nights == 1 ? " diária" : " diárias").append('\n')
                .append("Diárias: ").append(Formats.money(subtotal)).append('\n');
        if (discount.signum() > 0) {
            text.append("Desconto: ").append(Formats.money(discount)).append('\n');
        }
        return text.append("Total: ").append(Formats.money(total)).toString();
    }
}
