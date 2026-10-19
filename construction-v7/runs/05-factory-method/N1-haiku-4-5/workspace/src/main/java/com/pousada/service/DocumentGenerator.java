package com.pousada.service;

import com.pousada.domain.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class DocumentGenerator {
    private static final DateTimeFormatter PT_DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter EN_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public static Document generateDocument(Reservation reservation, Room room) {
        GuestIdentification id = reservation.getGuestIdentification();

        if (id instanceof CpfIdentification cpfId) {
            return generateComprovante(reservation, room, cpfId);
        } else if (id instanceof CnpjIdentification cnpjId) {
            return generateFatura(reservation, room, cnpjId);
        } else if (id instanceof PassportIdentification passportId) {
            return generateBookingReceipt(reservation, room, passportId);
        }

        throw new IllegalArgumentException("Unknown identification type");
    }

    private static Document generateComprovante(Reservation reservation, Room room, CpfIdentification cpfId) {
        String checkIn = reservation.getCheckIn().format(PT_DATE_FORMAT);
        String checkOut = reservation.getCheckOut().format(PT_DATE_FORMAT);

        String text = String.format(
            "Comprovante de reserva nº %d%n" +
            "Hóspede: %s (CPF %s)%n" +
            "Quarto: %d (%s)%n" +
            "Entrada: %s | Saída: %s | %s%n" +
            "Diárias: R$ %s%n" +
            "%s" +
            "Total: R$ %s",
            reservation.getId(),
            reservation.getGuestName(),
            cpfId.guestCpf,
            room.getNumber(),
            room.getType(),
            checkIn,
            checkOut,
            nightsLabel(reservation.getNights()),
            formatPrice(reservation.getSubtotal()),
            discountLine(reservation.getDiscount()),
            formatPrice(reservation.getTotal())
        );

        return new Document("Comprovante de reserva", reservation.getId(),
            reservation.getGuestName(), cpfId, null, null,
            room.getNumber(), room.getType(), checkIn, checkOut,
            reservation.getNights(), reservation.getSubtotal(),
            reservation.getDiscount(), reservation.getTotal(), text);
    }

    private static Document generateFatura(Reservation reservation, Room room, CnpjIdentification cnpjId) {
        String checkIn = reservation.getCheckIn().format(PT_DATE_FORMAT);
        String checkOut = reservation.getCheckOut().format(PT_DATE_FORMAT);
        BigDecimal issRetained = reservation.getTotal().multiply(new BigDecimal("0.05"))
            .setScale(2, RoundingMode.HALF_EVEN);

        String text = String.format(
            "Fatura de hospedagem nº %d%n" +
            "Empresa: %s (CNPJ %s)%n" +
            "Hóspede: %s%n" +
            "Quarto: %d (%s)%n" +
            "Entrada: %s | Saída: %s | %s%n" +
            "Diárias: R$ %s%n" +
            "%s" +
            "Total: R$ %s%n" +
            "ISS retido (5%%): R$ %s",
            reservation.getId(),
            cnpjId.companyName,
            cnpjId.companyCnpj,
            reservation.getGuestName(),
            room.getNumber(),
            room.getType(),
            checkIn,
            checkOut,
            nightsLabel(reservation.getNights()),
            formatPrice(reservation.getSubtotal()),
            discountLine(reservation.getDiscount()),
            formatPrice(reservation.getTotal()),
            formatPrice(issRetained)
        );

        return new Document("Fatura de hospedagem", reservation.getId(),
            reservation.getGuestName(), null, cnpjId, null,
            room.getNumber(), room.getType(), checkIn, checkOut,
            reservation.getNights(), reservation.getSubtotal(),
            reservation.getDiscount(), reservation.getTotal(), text, issRetained);
    }

    private static Document generateBookingReceipt(Reservation reservation, Room room, PassportIdentification passportId) {
        String checkIn = reservation.getCheckIn().format(EN_DATE_FORMAT);
        String checkOut = reservation.getCheckOut().format(EN_DATE_FORMAT);

        String text = String.format(
            "Booking receipt no. %d%n" +
            "Guest: %s (passport %s, %s)%n" +
            "Room: %d (%s)%n" +
            "Check-in: %s | Check-out: %s | %s%n" +
            "Nights: BRL %s%n" +
            "%s" +
            "Total: BRL %s",
            reservation.getId(),
            reservation.getGuestName(),
            passportId.guestPassport,
            passportId.guestCountry,
            room.getNumber(),
            room.getType(),
            checkIn,
            checkOut,
            englishNightsLabel(reservation.getNights()),
            formatPriceEnglish(reservation.getSubtotal()),
            englishDiscountLine(reservation.getDiscount()),
            formatPriceEnglish(reservation.getTotal())
        );

        return new Document("Booking receipt", reservation.getId(),
            reservation.getGuestName(), null, null, passportId,
            room.getNumber(), room.getType(), checkIn, checkOut,
            reservation.getNights(), reservation.getSubtotal(),
            reservation.getDiscount(), reservation.getTotal(), text);
    }

    private static String nightsLabel(long nights) {
        return nights == 1 ? "1 diária" : nights + " diárias";
    }

    private static String englishNightsLabel(long nights) {
        return nights == 1 ? "1 night" : nights + " nights";
    }

    private static String discountLine(BigDecimal discount) {
        if (discount.compareTo(BigDecimal.ZERO) > 0) {
            return "Desconto: R$ " + formatPrice(discount) + "\n";
        }
        return "";
    }

    private static String englishDiscountLine(BigDecimal discount) {
        if (discount.compareTo(BigDecimal.ZERO) > 0) {
            return "Discount: BRL " + formatPriceEnglish(discount) + "\n";
        }
        return "";
    }

    private static String formatPrice(BigDecimal amount) {
        return String.format("%.2f", amount).replace(".", ",");
    }

    private static String formatPriceEnglish(BigDecimal amount) {
        return String.format("%.2f", amount).replace(",", ".");
    }
}
