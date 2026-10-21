package com.pousadamarealta.service;

import com.pousadamarealta.model.Document;
import com.pousadamarealta.model.GuestIdentification;
import com.pousadamarealta.model.Reservation;
import com.pousadamarealta.model.Room;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class DocumentGenerator {
    private final PricingService pricingService;
    private static final DateTimeFormatter PT_DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter EN_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public DocumentGenerator(PricingService pricingService) {
        this.pricingService = pricingService;
    }

    public Document generate(Reservation reservation, Room room) {
        BigDecimal issRetained = null;
        String guestCpf = null;
        String companyCnpj = null;
        String companyName = null;
        String guestPassport = null;
        String guestCountry = null;

        String text = "";
        String title = "";

        if (reservation.identification instanceof GuestIdentification.CPF cpf) {
            guestCpf = cpf.value;
            title = "Comprovante de reserva";
            text = generatePortugueseReceipt(reservation, room, cpf.value);
        } else if (reservation.identification instanceof GuestIdentification.CNPJ cnpj) {
            companyCnpj = cnpj.value;
            companyName = cnpj.companyName;
            title = "Fatura de hospedagem";
            issRetained = pricingService.calculateIssRetention(reservation.total);
            text = generateInvoice(reservation, room, cnpj.value, cnpj.companyName, issRetained);
        } else if (reservation.identification instanceof GuestIdentification.Passport passport) {
            guestPassport = passport.number;
            guestCountry = passport.country;
            title = "Booking receipt";
            text = generateBookingReceipt(reservation, room, passport.number, passport.country);
        }

        return new Document(title, reservation.id, reservation.guestName, guestCpf,
                           companyCnpj, companyName, guestPassport, guestCountry,
                           room.number, room.type, reservation.checkIn, reservation.checkOut,
                           reservation.nights, reservation.subtotal, reservation.discount,
                           reservation.total, text, issRetained);
    }

    private String generatePortugueseReceipt(Reservation reservation, Room room, String cpf) {
        String nightsLabel = reservation.nights == 1 ? "1 diária" : reservation.nights + " diárias";
        String discountLine = reservation.discount.compareTo(BigDecimal.ZERO) > 0
                ? String.format("Desconto: R$ %s\n", formatMoney(reservation.discount))
                : "";

        return String.format("Comprovante de reserva nº %d\n" +
                "Hóspede: %s (CPF %s)\n" +
                "Quarto: %d (%s)\n" +
                "Entrada: %s | Saída: %s | %s\n" +
                "Diárias: R$ %s\n" +
                "%sTotal: R$ %s",
                reservation.id, reservation.guestName, cpf, room.number, room.type,
                reservation.checkIn.format(PT_DATE_FORMAT), reservation.checkOut.format(PT_DATE_FORMAT),
                nightsLabel, formatMoney(reservation.subtotal), discountLine,
                formatMoney(reservation.total));
    }

    private String generateInvoice(Reservation reservation, Room room, String cnpj, String companyName, BigDecimal issRetained) {
        String nightsLabel = reservation.nights == 1 ? "1 diária" : reservation.nights + " diárias";
        String discountLine = reservation.discount.compareTo(BigDecimal.ZERO) > 0
                ? String.format("Desconto: R$ %s\n", formatMoney(reservation.discount))
                : "";

        return String.format("Fatura de hospedagem nº %d\n" +
                "Empresa: %s (CNPJ %s)\n" +
                "Hóspede: %s\n" +
                "Quarto: %d (%s)\n" +
                "Entrada: %s | Saída: %s | %s\n" +
                "Diárias: R$ %s\n" +
                "%sTotal: R$ %s\n" +
                "ISS retido (5%%): R$ %s",
                reservation.id, companyName, cnpj, reservation.guestName, room.number, room.type,
                reservation.checkIn.format(PT_DATE_FORMAT), reservation.checkOut.format(PT_DATE_FORMAT),
                nightsLabel, formatMoney(reservation.subtotal), discountLine,
                formatMoney(reservation.total), formatMoney(issRetained));
    }

    private String generateBookingReceipt(Reservation reservation, Room room, String passportNumber, String country) {
        String nightsLabel = reservation.nights == 1 ? "1 night" : reservation.nights + " nights";
        String discountLine = reservation.discount.compareTo(BigDecimal.ZERO) > 0
                ? String.format("Discount: BRL %s\n", formatMoneyEnglish(reservation.discount))
                : "";

        return String.format("Booking receipt no. %d\n" +
                "Guest: %s (passport %s, %s)\n" +
                "Room: %d (%s)\n" +
                "Check-in: %s | Check-out: %s | %s\n" +
                "Nights: BRL %s\n" +
                "%sTotal: BRL %s",
                reservation.id, reservation.guestName, passportNumber, country,
                room.number, room.type,
                reservation.checkIn.format(EN_DATE_FORMAT), reservation.checkOut.format(EN_DATE_FORMAT),
                nightsLabel, formatMoneyEnglish(reservation.subtotal), discountLine,
                formatMoneyEnglish(reservation.total));
    }

    private String formatMoney(BigDecimal value) {
        long integerPart = value.longValue();
        int decimalPart = value.remainder(BigDecimal.ONE).multiply(BigDecimal.valueOf(100)).intValue();
        String formatted = String.format("%,d", integerPart).replace(',', '.');
        return String.format("%s,%02d", formatted, decimalPart);
    }

    private String formatMoneyEnglish(BigDecimal value) {
        return String.format("%.2f", value);
    }
}
