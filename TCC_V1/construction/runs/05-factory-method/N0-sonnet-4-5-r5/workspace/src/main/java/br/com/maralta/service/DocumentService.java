package br.com.maralta.service;

import br.com.maralta.model.*;
import br.com.maralta.util.MoneyUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;

public class DocumentService {

    private static final DateTimeFormatter PT_DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter EN_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final BigDecimal ISS_RATE = new BigDecimal("0.05");

    public Document generateDocument(Reservation reservation, Room room) {
        Document doc = new Document();
        doc.setReservationId(reservation.getId());
        doc.setGuestName(reservation.getGuestName());
        doc.setRoomNumber(room.getNumber());
        doc.setRoomType(room.getType());
        doc.setCheckIn(reservation.getCheckIn());
        doc.setCheckOut(reservation.getCheckOut());
        doc.setNights(reservation.getNights());
        doc.setSubtotal(reservation.getSubtotal());
        doc.setDiscount(reservation.getDiscount());
        doc.setTotal(reservation.getTotal());

        GuestIdentification id = reservation.getIdentification();
        doc.setGuestCpf(id.getGuestCpf());
        doc.setCompanyCnpj(id.getCompanyCnpj());
        doc.setCompanyName(id.getCompanyName());
        doc.setGuestPassport(id.getGuestPassport());
        doc.setGuestCountry(id.getGuestCountry());

        if (id.isCpf()) {
            generateReceipt(doc);
        } else if (id.isCompany()) {
            generateInvoice(doc);
        } else if (id.isPassport()) {
            generateBookingReceipt(doc);
        }

        return doc;
    }

    private void generateReceipt(Document doc) {
        doc.setTitle("Comprovante de reserva");

        StringBuilder text = new StringBuilder();
        text.append("Comprovante de reserva nº ").append(doc.getReservationId()).append("\n");
        text.append("Hóspede: ").append(doc.getGuestName());
        text.append(" (CPF ").append(doc.getGuestCpf()).append(")\n");
        text.append("Quarto: ").append(doc.getRoomNumber());
        text.append(" (").append(doc.getRoomType()).append(")\n");
        text.append("Entrada: ").append(doc.getCheckIn().format(PT_DATE_FORMAT));
        text.append(" | Saída: ").append(doc.getCheckOut().format(PT_DATE_FORMAT));
        text.append(" | ").append(doc.getNights()).append(doc.getNights() == 1 ? " diária" : " diárias").append("\n");
        text.append("Diárias: ").append(MoneyUtils.formatBRL(doc.getSubtotal())).append("\n");

        if (doc.getDiscount().compareTo(BigDecimal.ZERO) > 0) {
            text.append("Desconto: ").append(MoneyUtils.formatBRL(doc.getDiscount())).append("\n");
        }

        text.append("Total: ").append(MoneyUtils.formatBRL(doc.getTotal()));

        doc.setText(text.toString());
    }

    private void generateInvoice(Document doc) {
        doc.setTitle("Fatura de hospedagem");

        BigDecimal iss = MoneyUtils.round(doc.getTotal().multiply(ISS_RATE));

        StringBuilder text = new StringBuilder();
        text.append("Fatura de hospedagem nº ").append(doc.getReservationId()).append("\n");
        text.append("Empresa: ").append(doc.getCompanyName());
        text.append(" (CNPJ ").append(doc.getCompanyCnpj()).append(")\n");
        text.append("Hóspede: ").append(doc.getGuestName()).append("\n");
        text.append("Quarto: ").append(doc.getRoomNumber());
        text.append(" (").append(doc.getRoomType()).append(")\n");
        text.append("Entrada: ").append(doc.getCheckIn().format(PT_DATE_FORMAT));
        text.append(" | Saída: ").append(doc.getCheckOut().format(PT_DATE_FORMAT));
        text.append(" | ").append(doc.getNights()).append(doc.getNights() == 1 ? " diária" : " diárias").append("\n");
        text.append("Diárias: ").append(MoneyUtils.formatBRL(doc.getSubtotal())).append("\n");

        if (doc.getDiscount().compareTo(BigDecimal.ZERO) > 0) {
            text.append("Desconto: ").append(MoneyUtils.formatBRL(doc.getDiscount())).append("\n");
        }

        text.append("Total: ").append(MoneyUtils.formatBRL(doc.getTotal())).append("\n");
        text.append("ISS retido (5%): ").append(MoneyUtils.formatBRL(iss));

        doc.setText(text.toString());
    }

    private void generateBookingReceipt(Document doc) {
        doc.setTitle("Booking receipt");

        StringBuilder text = new StringBuilder();
        text.append("Booking receipt no. ").append(doc.getReservationId()).append("\n");
        text.append("Guest: ").append(doc.getGuestName());
        text.append(" (passport ").append(doc.getGuestPassport());
        text.append(", ").append(doc.getGuestCountry()).append(")\n");
        text.append("Room: ").append(doc.getRoomNumber());
        text.append(" (").append(doc.getRoomType()).append(")\n");
        text.append("Check-in: ").append(doc.getCheckIn().format(EN_DATE_FORMAT));
        text.append(" | Check-out: ").append(doc.getCheckOut().format(EN_DATE_FORMAT));
        text.append(" | ").append(doc.getNights()).append(doc.getNights() == 1 ? " night" : " nights").append("\n");
        text.append("Nights: ").append(MoneyUtils.formatBRLInternational(doc.getSubtotal())).append("\n");

        if (doc.getDiscount().compareTo(BigDecimal.ZERO) > 0) {
            text.append("Discount: ").append(MoneyUtils.formatBRLInternational(doc.getDiscount())).append("\n");
        }

        text.append("Total: ").append(MoneyUtils.formatBRLInternational(doc.getTotal()));

        doc.setText(text.toString());
    }
}
