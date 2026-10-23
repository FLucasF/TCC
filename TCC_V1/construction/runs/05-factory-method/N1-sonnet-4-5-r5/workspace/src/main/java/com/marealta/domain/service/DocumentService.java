package com.marealta.domain.service;

import com.marealta.domain.model.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class DocumentService {
    private static final DateTimeFormatter PT_DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter EN_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final NumberFormat PT_CURRENCY_FORMAT = NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));
    private static final NumberFormat EN_CURRENCY_FORMAT;

    static {
        EN_CURRENCY_FORMAT = NumberFormat.getCurrencyInstance(Locale.US);
        EN_CURRENCY_FORMAT.setCurrency(java.util.Currency.getInstance("BRL"));
    }

    public Document generate(Reservation reservation, Room room) {
        String text = generateText(reservation, room);

        GuestIdentification guest = reservation.getGuest();
        String guestCpf = guest instanceof CpfGuest cpf ? cpf.guestCpf() : null;
        String companyCnpj = guest instanceof CompanyGuest company ? company.companyCnpj() : null;
        String companyName = guest instanceof CompanyGuest company ? company.companyName() : null;
        String guestPassport = guest instanceof PassportGuest passport ? passport.guestPassport() : null;
        String guestCountry = guest instanceof PassportGuest passport ? passport.guestCountry() : null;

        return new Document(
            text,
            reservation.getId(),
            guest.getGuestName(),
            guestCpf,
            companyCnpj,
            companyName,
            guestPassport,
            guestCountry,
            room.number(),
            room.type(),
            reservation.getCheckIn(),
            reservation.getCheckOut(),
            reservation.getNights(),
            reservation.getSubtotal(),
            reservation.getDiscount(),
            reservation.getTotal()
        );
    }

    private String generateText(Reservation reservation, Room room) {
        GuestIdentification guest = reservation.getGuest();

        return switch (guest) {
            case CpfGuest cpf -> generateComprovante(reservation, room, cpf);
            case CompanyGuest company -> generateFatura(reservation, room, company);
            case PassportGuest passport -> generateReceipt(reservation, room, passport);
        };
    }

    private String generateComprovante(Reservation reservation, Room room, CpfGuest guest) {
        StringBuilder sb = new StringBuilder();
        sb.append("Comprovante de reserva nº ").append(reservation.getId()).append("\n");
        sb.append("Hóspede: ").append(guest.guestName())
          .append(" (CPF ").append(guest.guestCpf()).append(")\n");
        sb.append("Quarto: ").append(room.number())
          .append(" (").append(room.type()).append(")\n");
        sb.append("Entrada: ").append(reservation.getCheckIn().format(PT_DATE_FORMAT))
          .append(" | Saída: ").append(reservation.getCheckOut().format(PT_DATE_FORMAT))
          .append(" | ").append(reservation.getNights())
          .append(reservation.getNights() == 1 ? " diária" : " diárias").append("\n");
        sb.append("Diárias: ").append(PT_CURRENCY_FORMAT.format(reservation.getSubtotal())).append("\n");

        if (reservation.getDiscount().compareTo(BigDecimal.ZERO) > 0) {
            sb.append("Desconto: ").append(PT_CURRENCY_FORMAT.format(reservation.getDiscount())).append("\n");
        }

        sb.append("Total: ").append(PT_CURRENCY_FORMAT.format(reservation.getTotal()));

        return sb.toString();
    }

    private String generateFatura(Reservation reservation, Room room, CompanyGuest guest) {
        StringBuilder sb = new StringBuilder();
        sb.append("Fatura de hospedagem nº ").append(reservation.getId()).append("\n");
        sb.append("Empresa: ").append(guest.companyName())
          .append(" (CNPJ ").append(guest.companyCnpj()).append(")\n");
        sb.append("Hóspede: ").append(guest.guestName()).append("\n");
        sb.append("Quarto: ").append(room.number())
          .append(" (").append(room.type()).append(")\n");
        sb.append("Entrada: ").append(reservation.getCheckIn().format(PT_DATE_FORMAT))
          .append(" | Saída: ").append(reservation.getCheckOut().format(PT_DATE_FORMAT))
          .append(" | ").append(reservation.getNights())
          .append(reservation.getNights() == 1 ? " diária" : " diárias").append("\n");
        sb.append("Diárias: ").append(PT_CURRENCY_FORMAT.format(reservation.getSubtotal())).append("\n");

        if (reservation.getDiscount().compareTo(BigDecimal.ZERO) > 0) {
            sb.append("Desconto: ").append(PT_CURRENCY_FORMAT.format(reservation.getDiscount())).append("\n");
        }

        sb.append("Total: ").append(PT_CURRENCY_FORMAT.format(reservation.getTotal())).append("\n");

        BigDecimal iss = reservation.getTotal().multiply(new BigDecimal("0.05"))
            .setScale(2, RoundingMode.HALF_EVEN);
        sb.append("ISS retido (5%): ").append(PT_CURRENCY_FORMAT.format(iss));

        return sb.toString();
    }

    private String generateReceipt(Reservation reservation, Room room, PassportGuest guest) {
        StringBuilder sb = new StringBuilder();
        sb.append("Booking receipt no. ").append(reservation.getId()).append("\n");
        sb.append("Guest: ").append(guest.guestName())
          .append(" (passport ").append(guest.guestPassport())
          .append(", ").append(guest.guestCountry()).append(")\n");
        sb.append("Room: ").append(room.number())
          .append(" (").append(room.type()).append(")\n");
        sb.append("Check-in: ").append(reservation.getCheckIn().format(EN_DATE_FORMAT))
          .append(" | Check-out: ").append(reservation.getCheckOut().format(EN_DATE_FORMAT))
          .append(" | ").append(reservation.getNights())
          .append(reservation.getNights() == 1 ? " night" : " nights").append("\n");
        sb.append("Nights: ").append(formatBrlForEnglish(reservation.getSubtotal())).append("\n");

        if (reservation.getDiscount().compareTo(BigDecimal.ZERO) > 0) {
            sb.append("Discount: ").append(formatBrlForEnglish(reservation.getDiscount())).append("\n");
        }

        sb.append("Total: ").append(formatBrlForEnglish(reservation.getTotal()));

        return sb.toString();
    }

    private String formatBrlForEnglish(BigDecimal amount) {
        String formatted = EN_CURRENCY_FORMAT.format(amount);
        return formatted.replace("BRL", "BRL ");
    }
}
