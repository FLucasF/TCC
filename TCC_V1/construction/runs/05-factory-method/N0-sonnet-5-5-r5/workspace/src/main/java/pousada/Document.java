package pousada;

import java.math.BigDecimal;

/** Documento emitido ao hóspede (comprovante, fatura ou booking receipt). */
public record Document(
        String title,
        long reservationId,
        String guestName,
        String guestCpf,
        String companyCnpj,
        String companyName,
        String guestPassport,
        String guestCountry,
        int roomNumber,
        RoomType roomType,
        String checkIn,
        String checkOut,
        int nights,
        BigDecimal subtotal,
        BigDecimal discount,
        BigDecimal total,
        String text) {

    public static Document of(Reservation r) {
        Identification i = r.identification();
        return new Document(i.title(), r.id(), r.guestName(), i.cpf(), i.cnpj(), i.companyName(), i.passport(),
                i.country(), r.roomNumber(), r.roomType(), r.checkIn().toString(), r.checkOut().toString(),
                r.nights(), r.subtotal(), r.discount(), r.total(), i.render(r));
    }
}
