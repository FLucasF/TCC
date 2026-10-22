package com.pousadamaralta.domain.identification;

import com.pousadamaralta.domain.model.RoomType;
import java.math.BigDecimal;
import java.time.LocalDate;

public abstract class GuestIdentification {

    public String generateDocument(
            Long reservationId,
            String guestName,
            String roomNumber,
            RoomType roomType,
            LocalDate checkIn,
            LocalDate checkOut,
            int nights,
            BigDecimal subtotal,
            BigDecimal discount,
            BigDecimal total
    ) {
        StringBuilder doc = new StringBuilder();

        doc.append(getDocumentTitle()).append(" nº ").append(reservationId).append("\n");
        doc.append(formatGuestLine(guestName)).append("\n");
        doc.append(formatRoomLine(roomNumber, roomType)).append("\n");
        doc.append(formatStayLine(checkIn, checkOut, nights)).append("\n");
        doc.append(formatSubtotalLine(subtotal, nights)).append("\n");

        if (discount.compareTo(BigDecimal.ZERO) > 0) {
            doc.append(formatDiscountLine(discount)).append("\n");
        }

        doc.append(formatTotalLine(total)).append("\n");

        String extraInfo = getExtraInfo(total);
        if (extraInfo != null && !extraInfo.isEmpty()) {
            doc.append(extraInfo).append("\n");
        }

        return doc.toString().trim();
    }

    protected abstract String getDocumentTitle();

    protected abstract String formatGuestLine(String guestName);

    protected String formatRoomLine(String roomNumber, RoomType roomType) {
        return "Quarto: " + roomNumber + " (" + roomType.name() + ")";
    }

    protected String formatStayLine(LocalDate checkIn, LocalDate checkOut, int nights) {
        String nightsLabel = nights == 1 ? "1 diária" : nights + " diárias";
        return "Entrada: " + formatDate(checkIn) + " | Saída: " + formatDate(checkOut) + " | " + nightsLabel;
    }

    protected String formatSubtotalLine(BigDecimal subtotal, int nights) {
        return "Diárias: " + formatMoney(subtotal);
    }

    protected String formatDiscountLine(BigDecimal discount) {
        return "Desconto: " + formatMoney(discount);
    }

    protected String formatTotalLine(BigDecimal total) {
        return "Total: " + formatMoney(total);
    }

    protected String getExtraInfo(BigDecimal total) {
        return null;
    }

    protected abstract String formatMoney(BigDecimal amount);

    protected abstract String formatDate(LocalDate date);

    public abstract String getCpf();
    public abstract String getCnpj();
    public abstract String getCompanyName();
    public abstract String getPassport();
    public abstract String getCountry();
}
