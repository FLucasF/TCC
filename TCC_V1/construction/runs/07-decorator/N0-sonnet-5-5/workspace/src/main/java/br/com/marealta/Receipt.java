package br.com.marealta;

import br.com.marealta.Domain.Reservation;
import br.com.marealta.Domain.RoomType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/** Comprovante de reserva. */
public record Receipt(
        long reservationId, String guestName, String guestCpf, int roomNumber, RoomType roomType,
        LocalDate checkIn, LocalDate checkOut, int nights,
        BigDecimal subtotal, BigDecimal discount, BigDecimal total) {

    public static final String TITLE = "Comprovante de reserva";
    private static final DateTimeFormatter BR_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public static Receipt of(Reservation r, RoomType type) {
        return new Receipt(r.id(), r.guestName(), r.guestCpf(), r.roomNumber(), type,
                r.checkIn(), r.checkOut(), r.nights(), r.subtotal(), r.discount(), r.total());
    }

    public String render() {
        StringBuilder sb = new StringBuilder();
        sb.append(TITLE).append(" nº ").append(reservationId).append('\n');
        sb.append("Hóspede: ").append(guestName).append(" (CPF ").append(guestCpf).append(")\n");
        sb.append("Quarto: ").append(roomNumber).append(" (").append(roomType).append(")\n");
        sb.append("Entrada: ").append(BR_DATE.format(checkIn))
                .append(" | Saída: ").append(BR_DATE.format(checkOut))
                .append(" | ").append(nights).append(nights == 1 ? " diária" : " diárias").append('\n');
        sb.append("Diárias: ").append(brl(subtotal)).append('\n');
        if (discount.signum() > 0) {
            sb.append("Desconto: ").append(brl(discount)).append('\n');
        }
        sb.append("Total: ").append(brl(total));
        return sb.toString();
    }

    /** 1234.56 -> "R$ 1.234,56" (sem depender do locale da JVM). */
    public static String brl(BigDecimal v) {
        String plain = Pricing.money(v).abs().toPlainString();
        int dot = plain.indexOf('.');
        String ints = plain.substring(0, dot);
        StringBuilder grouped = new StringBuilder();
        for (int i = 0; i < ints.length(); i++) {
            if (i > 0 && (ints.length() - i) % 3 == 0) {
                grouped.append('.');
            }
            grouped.append(ints.charAt(i));
        }
        return (v.signum() < 0 ? "-R$ " : "R$ ") + grouped + "," + plain.substring(dot + 1);
    }
}
