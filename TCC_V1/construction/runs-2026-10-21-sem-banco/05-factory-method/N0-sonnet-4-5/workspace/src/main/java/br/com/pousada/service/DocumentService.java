package br.com.pousada.service;

import br.com.pousada.model.Document;
import br.com.pousada.model.Reservation;
import br.com.pousada.model.Room;
import br.com.pousada.util.*;
import java.math.BigDecimal;

public class DocumentService {

    public Document generateDocument(Reservation reservation, Room room) {
        String text;
        String title;

        if (reservation.getGuestCpf() != null) {
            title = "Comprovante de reserva";
            text = generateComprovante(reservation, room);
        } else if (reservation.getCompanyCnpj() != null) {
            title = "Fatura de hospedagem";
            text = generateFatura(reservation, room);
        } else {
            title = "Booking receipt";
            text = generateBookingReceipt(reservation, room);
        }

        return new Document(
            title,
            reservation.getId(),
            reservation.getGuestName(),
            reservation.getGuestCpf(),
            reservation.getCompanyCnpj(),
            reservation.getCompanyName(),
            reservation.getGuestPassport(),
            reservation.getGuestCountry(),
            room.getNumber(),
            room.getType(),
            reservation.getCheckIn(),
            reservation.getCheckOut(),
            reservation.getNights(),
            reservation.getSubtotal(),
            reservation.getDiscount(),
            reservation.getTotal(),
            text
        );
    }

    private String generateComprovante(Reservation reservation, Room room) {
        StringBuilder sb = new StringBuilder();
        sb.append("Comprovante de reserva nº ").append(reservation.getId()).append("\n");
        sb.append("Hóspede: ").append(reservation.getGuestName())
          .append(" (CPF ").append(CpfFormatter.format(reservation.getGuestCpf())).append(")\n");
        sb.append("Quarto: ").append(room.getNumber()).append(" (").append(room.getType()).append(")\n");
        sb.append("Entrada: ").append(DateFormatter.formatPtBr(reservation.getCheckIn()))
          .append(" | Saída: ").append(DateFormatter.formatPtBr(reservation.getCheckOut()))
          .append(" | ").append(reservation.getNights())
          .append(reservation.getNights() == 1 ? " diária" : " diárias").append("\n");
        sb.append("Diárias: ").append(MoneyFormatter.formatPtBr(reservation.getSubtotal())).append("\n");

        if (reservation.getDiscount().compareTo(BigDecimal.ZERO) > 0) {
            sb.append("Desconto: ").append(MoneyFormatter.formatPtBr(reservation.getDiscount())).append("\n");
        }

        sb.append("Total: ").append(MoneyFormatter.formatPtBr(reservation.getTotal()));

        return sb.toString();
    }

    private String generateFatura(Reservation reservation, Room room) {
        StringBuilder sb = new StringBuilder();
        sb.append("Fatura de hospedagem nº ").append(reservation.getId()).append("\n");
        sb.append("Empresa: ").append(reservation.getCompanyName())
          .append(" (CNPJ ").append(CnpjFormatter.format(reservation.getCompanyCnpj())).append(")\n");
        sb.append("Hóspede: ").append(reservation.getGuestName()).append("\n");
        sb.append("Quarto: ").append(room.getNumber()).append(" (").append(room.getType()).append(")\n");
        sb.append("Entrada: ").append(DateFormatter.formatPtBr(reservation.getCheckIn()))
          .append(" | Saída: ").append(DateFormatter.formatPtBr(reservation.getCheckOut()))
          .append(" | ").append(reservation.getNights())
          .append(reservation.getNights() == 1 ? " diária" : " diárias").append("\n");
        sb.append("Diárias: ").append(MoneyFormatter.formatPtBr(reservation.getSubtotal())).append("\n");

        if (reservation.getDiscount().compareTo(BigDecimal.ZERO) > 0) {
            sb.append("Desconto: ").append(MoneyFormatter.formatPtBr(reservation.getDiscount())).append("\n");
        }

        sb.append("Total: ").append(MoneyFormatter.formatPtBr(reservation.getTotal())).append("\n");
        sb.append("ISS retido (5%): ").append(MoneyFormatter.formatPtBr(PriceCalculator.calculateIss(reservation.getTotal())));

        return sb.toString();
    }

    private String generateBookingReceipt(Reservation reservation, Room room) {
        StringBuilder sb = new StringBuilder();
        sb.append("Booking receipt no. ").append(reservation.getId()).append("\n");
        sb.append("Guest: ").append(reservation.getGuestName())
          .append(" (passport ").append(reservation.getGuestPassport())
          .append(", ").append(reservation.getGuestCountry()).append(")\n");
        sb.append("Room: ").append(room.getNumber()).append(" (").append(room.getType()).append(")\n");
        sb.append("Check-in: ").append(DateFormatter.formatEn(reservation.getCheckIn()))
          .append(" | Check-out: ").append(DateFormatter.formatEn(reservation.getCheckOut()))
          .append(" | ").append(reservation.getNights())
          .append(reservation.getNights() == 1 ? " night" : " nights").append("\n");
        sb.append("Nights: ").append(MoneyFormatter.formatEn(reservation.getSubtotal())).append("\n");

        if (reservation.getDiscount().compareTo(BigDecimal.ZERO) > 0) {
            sb.append("Discount: ").append(MoneyFormatter.formatEn(reservation.getDiscount())).append("\n");
        }

        sb.append("Total: ").append(MoneyFormatter.formatEn(reservation.getTotal()));

        return sb.toString();
    }
}
