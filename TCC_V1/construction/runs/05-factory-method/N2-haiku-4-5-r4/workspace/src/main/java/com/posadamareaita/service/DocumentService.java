package com.posadamareaita.service;

import com.posadamareaita.domain.Reservation;
import com.posadamareaita.domain.Room;
import com.posadamareaita.util.FormattingUtil;
import com.posadamareaita.util.MoneyUtil;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

public class DocumentService {
    public static String generateReceiptPT(Reservation res, Room room) {
        StringBuilder sb = new StringBuilder();
        sb.append("Comprovante de reserva nº ").append(res.getId()).append("\n");
        sb.append("Hóspede: ").append(res.getGuestName());

        if (res.getGuestCpf() != null) {
            sb.append(" (CPF ").append(res.getGuestCpf()).append(")");
        }
        sb.append("\n");

        sb.append("Quarto: ").append(room.getNumber()).append(" (").append(room.getType().getDisplayName()).append(")\n");
        sb.append("Entrada: ").append(FormattingUtil.formatDatePT(res.getCheckIn()));
        sb.append(" | Saída: ").append(FormattingUtil.formatDatePT(res.getCheckOut()));
        sb.append(" | ").append(FormattingUtil.formatNightsPT(res.getNights())).append("\n");
        sb.append("Diárias: ").append(FormattingUtil.formatMoneyPT(res.getSubtotal())).append("\n");

        if (res.getDiscount().compareTo(BigDecimal.ZERO) > 0) {
            sb.append("Desconto: ").append(FormattingUtil.formatMoneyPT(res.getDiscount())).append("\n");
        }

        sb.append("Total: ").append(FormattingUtil.formatMoneyPT(res.getTotal())).append("\n");

        return sb.toString();
    }

    public static String generateInvoice(Reservation res, Room room) {
        BigDecimal iss = res.getTotal().multiply(new BigDecimal("0.05"));
        iss = MoneyUtil.round(iss);

        StringBuilder sb = new StringBuilder();
        sb.append("Fatura de hospedagem nº ").append(res.getId()).append("\n");
        sb.append("Empresa: ").append(res.getCompanyName());
        sb.append(" (CNPJ ").append(res.getCompanyCnpj()).append(")\n");
        sb.append("Hóspede: ").append(res.getGuestName()).append("\n");
        sb.append("Quarto: ").append(room.getNumber()).append(" (").append(room.getType().getDisplayName()).append(")\n");
        sb.append("Entrada: ").append(FormattingUtil.formatDatePT(res.getCheckIn()));
        sb.append(" | Saída: ").append(FormattingUtil.formatDatePT(res.getCheckOut()));
        sb.append(" | ").append(FormattingUtil.formatNightsPT(res.getNights())).append("\n");
        sb.append("Diárias: ").append(FormattingUtil.formatMoneyPT(res.getSubtotal())).append("\n");

        if (res.getDiscount().compareTo(BigDecimal.ZERO) > 0) {
            sb.append("Desconto: ").append(FormattingUtil.formatMoneyPT(res.getDiscount())).append("\n");
        }

        sb.append("Total: ").append(FormattingUtil.formatMoneyPT(res.getTotal())).append("\n");
        sb.append("ISS retido (5%): ").append(FormattingUtil.formatMoneyPT(iss)).append("\n");

        return sb.toString();
    }

    public static String generateBookingReceipt(Reservation res, Room room) {
        StringBuilder sb = new StringBuilder();
        sb.append("Booking receipt no. ").append(res.getId()).append("\n");
        sb.append("Guest: ").append(res.getGuestName());

        if (res.getGuestPassport() != null) {
            sb.append(" (passport ").append(res.getGuestPassport()).append(", ").append(res.getGuestCountry()).append(")");
        }
        sb.append("\n");

        sb.append("Room: ").append(room.getNumber()).append(" (").append(room.getType().getDisplayName()).append(")\n");
        sb.append("Check-in: ").append(FormattingUtil.formatDateEN(res.getCheckIn()));
        sb.append(" | Check-out: ").append(FormattingUtil.formatDateEN(res.getCheckOut()));
        sb.append(" | ").append(FormattingUtil.formatNightsEN(res.getNights())).append("\n");
        sb.append("Nights: ").append(FormattingUtil.formatMoneyEN(res.getSubtotal())).append("\n");

        if (res.getDiscount().compareTo(BigDecimal.ZERO) > 0) {
            sb.append("Discount: ").append(FormattingUtil.formatMoneyEN(res.getDiscount())).append("\n");
        }

        sb.append("Total: ").append(FormattingUtil.formatMoneyEN(res.getTotal())).append("\n");

        return sb.toString();
    }

    public static Map<String, Object> generateDocumentResponse(Reservation res, Room room) {
        Map<String, Object> response = new HashMap<>();
        response.put("title", "Comprovante de reserva");
        response.put("reservationId", res.getId());
        response.put("guestName", res.getGuestName());
        response.put("guestCpf", res.getGuestCpf());
        response.put("companyCnpj", res.getCompanyCnpj());
        response.put("companyName", res.getCompanyName());
        response.put("guestPassport", res.getGuestPassport());
        response.put("guestCountry", res.getGuestCountry());
        response.put("roomNumber", room.getNumber());
        response.put("roomType", room.getType().getDisplayName());
        response.put("checkIn", FormattingUtil.formatDateISO(res.getCheckIn()));
        response.put("checkOut", FormattingUtil.formatDateISO(res.getCheckOut()));
        response.put("nights", res.getNights());
        response.put("subtotal", res.getSubtotal());
        response.put("discount", res.getDiscount().compareTo(BigDecimal.ZERO) > 0 ? res.getDiscount() : null);
        response.put("total", res.getTotal());

        String text;
        if (res.getGuestCpf() != null) {
            text = generateReceiptPT(res, room);
        } else if (res.getCompanyCnpj() != null) {
            text = generateInvoice(res, room);
        } else {
            text = generateBookingReceipt(res, room);
        }
        response.put("text", text);

        return response;
    }
}
