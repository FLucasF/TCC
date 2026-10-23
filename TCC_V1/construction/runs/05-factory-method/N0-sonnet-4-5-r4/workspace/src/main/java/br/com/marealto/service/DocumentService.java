package br.com.marealto.service;

import br.com.marealto.model.GuestIdentification;
import br.com.marealto.model.Reservation;
import br.com.marealto.model.RoomType;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class DocumentService {
    private final PricingService pricingService;
    private final NumberFormat brFormat;
    private final NumberFormat enFormat;
    private final DateTimeFormatter brDateFormat;
    private final DateTimeFormatter enDateFormat;

    public DocumentService(PricingService pricingService) {
        this.pricingService = pricingService;
        this.brFormat = NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));
        this.enFormat = NumberFormat.getInstance(Locale.US);
        this.enFormat.setMinimumFractionDigits(2);
        this.enFormat.setMaximumFractionDigits(2);
        this.brDateFormat = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        this.enDateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    }

    public String generateDocument(Reservation reservation, RoomType roomType) {
        GuestIdentification id = reservation.getGuestIdentification();

        if (id instanceof GuestIdentification.Individual) {
            return generateReceipt(reservation, roomType);
        } else if (id instanceof GuestIdentification.Company) {
            return generateInvoice(reservation, roomType);
        } else if (id instanceof GuestIdentification.Foreign) {
            return generateBookingReceipt(reservation, roomType);
        }

        throw new IllegalStateException("Unknown identification type");
    }

    private String generateReceipt(Reservation reservation, RoomType roomType) {
        StringBuilder sb = new StringBuilder();
        GuestIdentification.Individual individual = (GuestIdentification.Individual) reservation.getGuestIdentification();

        sb.append("Comprovante de reserva nº ").append(reservation.getId()).append("\n");
        sb.append("Hóspede: ").append(reservation.getGuestName())
          .append(" (CPF ").append(formatCpf(individual.cpf())).append(")\n");
        sb.append("Quarto: ").append(reservation.getRoomNumber())
          .append(" (").append(roomType.name()).append(")\n");
        sb.append("Entrada: ").append(reservation.getCheckIn().format(brDateFormat))
          .append(" | Saída: ").append(reservation.getCheckOut().format(brDateFormat))
          .append(" | ").append(formatNights(reservation.getNights())).append("\n");
        sb.append("Diárias: ").append(brFormat.format(reservation.getSubtotal())).append("\n");

        if (reservation.getDiscount().compareTo(BigDecimal.ZERO) > 0) {
            sb.append("Desconto: ").append(brFormat.format(reservation.getDiscount())).append("\n");
        }

        sb.append("Total: ").append(brFormat.format(reservation.getTotal()));

        return sb.toString();
    }

    private String generateInvoice(Reservation reservation, RoomType roomType) {
        StringBuilder sb = new StringBuilder();
        GuestIdentification.Company company = (GuestIdentification.Company) reservation.getGuestIdentification();

        sb.append("Fatura de hospedagem nº ").append(reservation.getId()).append("\n");
        sb.append("Empresa: ").append(company.companyName())
          .append(" (CNPJ ").append(formatCnpj(company.cnpj())).append(")\n");
        sb.append("Hóspede: ").append(reservation.getGuestName()).append("\n");
        sb.append("Quarto: ").append(reservation.getRoomNumber())
          .append(" (").append(roomType.name()).append(")\n");
        sb.append("Entrada: ").append(reservation.getCheckIn().format(brDateFormat))
          .append(" | Saída: ").append(reservation.getCheckOut().format(brDateFormat))
          .append(" | ").append(formatNights(reservation.getNights())).append("\n");
        sb.append("Diárias: ").append(brFormat.format(reservation.getSubtotal())).append("\n");

        if (reservation.getDiscount().compareTo(BigDecimal.ZERO) > 0) {
            sb.append("Desconto: ").append(brFormat.format(reservation.getDiscount())).append("\n");
        }

        sb.append("Total: ").append(brFormat.format(reservation.getTotal())).append("\n");

        BigDecimal iss = pricingService.calculateIss(reservation.getTotal());
        sb.append("ISS retido (5%): ").append(brFormat.format(iss));

        return sb.toString();
    }

    private String generateBookingReceipt(Reservation reservation, RoomType roomType) {
        StringBuilder sb = new StringBuilder();
        GuestIdentification.Foreign foreign = (GuestIdentification.Foreign) reservation.getGuestIdentification();

        sb.append("Booking receipt no. ").append(reservation.getId()).append("\n");
        sb.append("Guest: ").append(reservation.getGuestName())
          .append(" (passport ").append(foreign.passport())
          .append(", ").append(foreign.country()).append(")\n");
        sb.append("Room: ").append(reservation.getRoomNumber())
          .append(" (").append(roomType.name()).append(")\n");
        sb.append("Check-in: ").append(reservation.getCheckIn().format(enDateFormat))
          .append(" | Check-out: ").append(reservation.getCheckOut().format(enDateFormat))
          .append(" | ").append(formatNightsEn(reservation.getNights())).append("\n");
        sb.append("Nights: BRL ").append(enFormat.format(reservation.getSubtotal())).append("\n");

        if (reservation.getDiscount().compareTo(BigDecimal.ZERO) > 0) {
            sb.append("Discount: BRL ").append(enFormat.format(reservation.getDiscount())).append("\n");
        }

        sb.append("Total: BRL ").append(enFormat.format(reservation.getTotal()));

        return sb.toString();
    }

    public String generateConfirmationEmail(Reservation reservation, RoomType roomType) {
        String document = generateDocument(reservation, roomType);

        StringBuilder sb = new StringBuilder();
        sb.append("Olá, ").append(reservation.getGuestName()).append("!\n\n");
        sb.append("Sua reserva na Pousada Maré Alta está confirmada. Estes são os dados dela:\n\n");
        sb.append(document).append("\n\n");
        sb.append("A entrada é a partir das 14h, e a saída, até as 12h.\n");
        sb.append("Até breve!");

        return sb.toString();
    }

    private String formatNights(int nights) {
        return nights == 1 ? "1 diária" : nights + " diárias";
    }

    private String formatNightsEn(int nights) {
        return nights == 1 ? "1 night" : nights + " nights";
    }

    private String formatCpf(String cpf) {
        if (cpf.length() == 11) {
            return cpf.substring(0, 3) + "." + cpf.substring(3, 6) + "." +
                   cpf.substring(6, 9) + "-" + cpf.substring(9);
        }
        return cpf;
    }

    private String formatCnpj(String cnpj) {
        if (cnpj.length() == 14) {
            return cnpj.substring(0, 2) + "." + cnpj.substring(2, 5) + "." +
                   cnpj.substring(5, 8) + "/" + cnpj.substring(8, 12) + "-" + cnpj.substring(12);
        }
        return cnpj;
    }
}
