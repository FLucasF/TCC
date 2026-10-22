package com.pousada.maralta.service;

import com.pousada.maralta.domain.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class DocumentGenerator {
    private static final DecimalFormat BRL_FORMAT;
    private static final DecimalFormat BRL_EN_FORMAT;
    private static final DateTimeFormatter PT_DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter EN_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    static {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(new Locale("pt", "BR"));
        symbols.setGroupingSeparator('.');
        symbols.setDecimalSeparator(',');
        BRL_FORMAT = new DecimalFormat("#,##0.00", symbols);

        DecimalFormatSymbols enSymbols = new DecimalFormatSymbols(Locale.US);
        enSymbols.setGroupingSeparator(',');
        enSymbols.setDecimalSeparator('.');
        BRL_EN_FORMAT = new DecimalFormat("#,##0.00", enSymbols);
    }

    public ReservationDocument generate(Reservation reservation, Room room) {
        String text = reservation.getIdentification().accept(new DocumentTextVisitor(reservation, room));

        return reservation.getIdentification().accept(new DocumentBuilder(reservation, room, text));
    }

    private static class DocumentBuilder implements GuestIdentificationVisitor<ReservationDocument> {
        private final Reservation reservation;
        private final Room room;
        private final String text;

        DocumentBuilder(Reservation reservation, Room room, String text) {
            this.reservation = reservation;
            this.room = room;
            this.text = text;
        }

        @Override
        public ReservationDocument visitCpf(CpfIdentification cpf) {
            return new ReservationDocument(
                "Comprovante de reserva",
                reservation.getId(),
                reservation.getGuestName(),
                cpf.cpf(),
                null,
                null,
                null,
                null,
                room.number(),
                room.type(),
                reservation.getCheckIn(),
                reservation.getCheckOut(),
                reservation.getNights(),
                reservation.getSubtotal(),
                reservation.getDiscount(),
                reservation.getTotal(),
                text
            );
        }

        @Override
        public ReservationDocument visitCnpj(CnpjIdentification cnpj) {
            return new ReservationDocument(
                "Fatura de hospedagem",
                reservation.getId(),
                reservation.getGuestName(),
                null,
                cnpj.cnpj(),
                cnpj.companyName(),
                null,
                null,
                room.number(),
                room.type(),
                reservation.getCheckIn(),
                reservation.getCheckOut(),
                reservation.getNights(),
                reservation.getSubtotal(),
                reservation.getDiscount(),
                reservation.getTotal(),
                text
            );
        }

        @Override
        public ReservationDocument visitPassport(PassportIdentification passport) {
            return new ReservationDocument(
                "Booking receipt",
                reservation.getId(),
                reservation.getGuestName(),
                null,
                null,
                null,
                passport.passport(),
                passport.country(),
                room.number(),
                room.type(),
                reservation.getCheckIn(),
                reservation.getCheckOut(),
                reservation.getNights(),
                reservation.getSubtotal(),
                reservation.getDiscount(),
                reservation.getTotal(),
                text
            );
        }
    }

    private static class DocumentTextVisitor implements GuestIdentificationVisitor<String> {
        private final Reservation reservation;
        private final Room room;

        DocumentTextVisitor(Reservation reservation, Room room) {
            this.reservation = reservation;
            this.room = room;
        }

        @Override
        public String visitCpf(CpfIdentification cpf) {
            StringBuilder sb = new StringBuilder();
            sb.append("Comprovante de reserva nº ").append(reservation.getId()).append("\n");
            sb.append("Hóspede: ").append(reservation.getGuestName())
              .append(" (CPF ").append(cpf.cpf()).append(")\n");
            sb.append("Quarto: ").append(room.number()).append(" (").append(room.type()).append(")\n");
            sb.append("Entrada: ").append(reservation.getCheckIn().format(PT_DATE_FORMAT))
              .append(" | Saída: ").append(reservation.getCheckOut().format(PT_DATE_FORMAT))
              .append(" | ").append(reservation.getNights())
              .append(reservation.getNights() == 1 ? " diária" : " diárias").append("\n");
            sb.append("Diárias: R$ ").append(BRL_FORMAT.format(reservation.getSubtotal())).append("\n");

            if (reservation.getDiscount().compareTo(BigDecimal.ZERO) > 0) {
                sb.append("Desconto: R$ ").append(BRL_FORMAT.format(reservation.getDiscount())).append("\n");
            }

            sb.append("Total: R$ ").append(BRL_FORMAT.format(reservation.getTotal()));
            return sb.toString();
        }

        @Override
        public String visitCnpj(CnpjIdentification cnpj) {
            StringBuilder sb = new StringBuilder();
            sb.append("Fatura de hospedagem nº ").append(reservation.getId()).append("\n");
            sb.append("Empresa: ").append(cnpj.companyName())
              .append(" (CNPJ ").append(cnpj.cnpj()).append(")\n");
            sb.append("Hóspede: ").append(reservation.getGuestName()).append("\n");
            sb.append("Quarto: ").append(room.number()).append(" (").append(room.type()).append(")\n");
            sb.append("Entrada: ").append(reservation.getCheckIn().format(PT_DATE_FORMAT))
              .append(" | Saída: ").append(reservation.getCheckOut().format(PT_DATE_FORMAT))
              .append(" | ").append(reservation.getNights())
              .append(reservation.getNights() == 1 ? " diária" : " diárias").append("\n");
            sb.append("Diárias: R$ ").append(BRL_FORMAT.format(reservation.getSubtotal())).append("\n");

            if (reservation.getDiscount().compareTo(BigDecimal.ZERO) > 0) {
                sb.append("Desconto: R$ ").append(BRL_FORMAT.format(reservation.getDiscount())).append("\n");
            }

            sb.append("Total: R$ ").append(BRL_FORMAT.format(reservation.getTotal())).append("\n");

            BigDecimal iss = reservation.getTotal().multiply(new BigDecimal("0.05"))
                .setScale(2, RoundingMode.HALF_EVEN);
            sb.append("ISS retido (5%): R$ ").append(BRL_FORMAT.format(iss));

            return sb.toString();
        }

        @Override
        public String visitPassport(PassportIdentification passport) {
            StringBuilder sb = new StringBuilder();
            sb.append("Booking receipt no. ").append(reservation.getId()).append("\n");
            sb.append("Guest: ").append(reservation.getGuestName())
              .append(" (passport ").append(passport.passport())
              .append(", ").append(passport.country()).append(")\n");
            sb.append("Room: ").append(room.number()).append(" (").append(room.type()).append(")\n");
            sb.append("Check-in: ").append(reservation.getCheckIn().format(EN_DATE_FORMAT))
              .append(" | Check-out: ").append(reservation.getCheckOut().format(EN_DATE_FORMAT))
              .append(" | ").append(reservation.getNights())
              .append(reservation.getNights() == 1 ? " night" : " nights").append("\n");
            sb.append("Nights: BRL ").append(BRL_EN_FORMAT.format(reservation.getSubtotal())).append("\n");

            if (reservation.getDiscount().compareTo(BigDecimal.ZERO) > 0) {
                sb.append("Discount: BRL ").append(BRL_EN_FORMAT.format(reservation.getDiscount())).append("\n");
            }

            sb.append("Total: BRL ").append(BRL_EN_FORMAT.format(reservation.getTotal()));
            return sb.toString();
        }
    }
}
