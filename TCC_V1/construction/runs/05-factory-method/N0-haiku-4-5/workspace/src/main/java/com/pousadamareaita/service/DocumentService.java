package com.pousadamareaita.service;

import com.pousadamareaita.model.Reservation;
import com.pousadamareaita.model.Document;
import com.pousadamareaita.model.Room;
import com.pousadamareaita.util.MoneyUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class DocumentService {
    private final RoomService roomService;

    public DocumentService(RoomService roomService) {
        this.roomService = roomService;
    }

    public Document generateDocument(Reservation reservation) throws Exception {
        Room room = roomService.getRoomByNumber(reservation.getRoomNumber());

        String title;
        String text;

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
            reservation.getRoomNumber(),
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
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        StringBuilder sb = new StringBuilder();

        sb.append("Comprovante de reserva nº ").append(reservation.getId()).append("\n");
        sb.append("Hóspede: ").append(reservation.getGuestName())
            .append(" (CPF ").append(formatCPF(reservation.getGuestCpf())).append(")\n");
        sb.append("Quarto: ").append(reservation.getRoomNumber()).append(" (").append(room.getType()).append(")\n");
        sb.append("Entrada: ").append(reservation.getCheckIn().format(dtf))
            .append(" | Saída: ").append(reservation.getCheckOut().format(dtf))
            .append(" | ").append(reservation.getNights())
            .append(reservation.getNights() == 1 ? " diária" : " diárias").append("\n");
        sb.append("Diárias: ").append(MoneyUtils.formatBRL(reservation.getSubtotal())).append("\n");

        if (reservation.getDiscount() != null && reservation.getDiscount().compareTo(BigDecimal.ZERO) > 0) {
            sb.append("Desconto: ").append(MoneyUtils.formatBRL(reservation.getDiscount())).append("\n");
        }

        sb.append("Total: ").append(MoneyUtils.formatBRL(reservation.getTotal()));

        return sb.toString();
    }

    private String generateFatura(Reservation reservation, Room room) {
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        BigDecimal iss = MoneyUtils.round(reservation.getTotal().multiply(new BigDecimal("0.05")));

        StringBuilder sb = new StringBuilder();

        sb.append("Fatura de hospedagem nº ").append(reservation.getId()).append("\n");
        sb.append("Empresa: ").append(reservation.getCompanyName())
            .append(" (CNPJ ").append(formatCNPJ(reservation.getCompanyCnpj())).append(")\n");
        sb.append("Hóspede: ").append(reservation.getGuestName()).append("\n");
        sb.append("Quarto: ").append(reservation.getRoomNumber()).append(" (").append(room.getType()).append(")\n");
        sb.append("Entrada: ").append(reservation.getCheckIn().format(dtf))
            .append(" | Saída: ").append(reservation.getCheckOut().format(dtf))
            .append(" | ").append(reservation.getNights())
            .append(reservation.getNights() == 1 ? " diária" : " diárias").append("\n");
        sb.append("Diárias: ").append(MoneyUtils.formatBRL(reservation.getSubtotal())).append("\n");

        if (reservation.getDiscount() != null && reservation.getDiscount().compareTo(BigDecimal.ZERO) > 0) {
            sb.append("Desconto: ").append(MoneyUtils.formatBRL(reservation.getDiscount())).append("\n");
        }

        sb.append("Total: ").append(MoneyUtils.formatBRL(reservation.getTotal())).append("\n");
        sb.append("ISS retido (5%): ").append(MoneyUtils.formatBRL(iss));

        return sb.toString();
    }

    private String generateBookingReceipt(Reservation reservation, Room room) {
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        StringBuilder sb = new StringBuilder();

        sb.append("Booking receipt no. ").append(reservation.getId()).append("\n");
        sb.append("Guest: ").append(reservation.getGuestName())
            .append(" (passport ").append(reservation.getGuestPassport())
            .append(", ").append(reservation.getGuestCountry()).append(")\n");
        sb.append("Room: ").append(reservation.getRoomNumber()).append(" (").append(room.getType()).append(")\n");
        sb.append("Check-in: ").append(reservation.getCheckIn().format(dtf))
            .append(" | Check-out: ").append(reservation.getCheckOut().format(dtf))
            .append(" | ").append(reservation.getNights())
            .append(reservation.getNights() == 1 ? " night" : " nights").append("\n");
        sb.append("Nights: ").append(MoneyUtils.formatBRL_EN_Proper(reservation.getSubtotal())).append("\n");

        if (reservation.getDiscount() != null && reservation.getDiscount().compareTo(BigDecimal.ZERO) > 0) {
            sb.append("Discount: ").append(MoneyUtils.formatBRL_EN_Proper(reservation.getDiscount())).append("\n");
        }

        sb.append("Total: ").append(MoneyUtils.formatBRL_EN_Proper(reservation.getTotal()));

        return sb.toString();
    }

    private String formatCPF(String cpf) {
        if (cpf == null || cpf.length() != 11) {
            return cpf;
        }
        return cpf.substring(0, 3) + "." + cpf.substring(3, 6) + "." + cpf.substring(6, 9) + "-" + cpf.substring(9);
    }

    private String formatCNPJ(String cnpj) {
        if (cnpj == null || cnpj.length() != 14) {
            return cnpj;
        }
        return cnpj.substring(0, 2) + "." + cnpj.substring(2, 5) + "." + cnpj.substring(5, 8) + "/" +
               cnpj.substring(8, 12) + "-" + cnpj.substring(12);
    }
}
