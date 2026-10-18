package hotel.documents;

import hotel.reservations.Identification;
import hotel.rooms.RoomType;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

public sealed interface DocumentText {
    String text();

    record CpfReceipt(
            long reservationId,
            String guestName,
            String guestCpf,
            int roomNumber,
            RoomType roomType,
            LocalDate checkIn,
            LocalDate checkOut,
            int nights,
            BigDecimal subtotal,
            BigDecimal discount,
            BigDecimal total)
            implements DocumentText {

        @Override
        public String text() {
            StringBuilder text = new StringBuilder();
            text.append("Comprovante de reserva nº ").append(reservationId).append('\n');
            text.append("Hóspede: ").append(guestName).append(" (CPF ").append(guestCpf).append(")\n");
            text.append("Quarto: ").append(roomNumber).append(" (").append(roomType).append(")\n");
            text.append("Entrada: ").append(TextFormat.date(checkIn))
                    .append(" | Saída: ").append(TextFormat.date(checkOut))
                    .append(" | ").append(nights).append(nights == 1 ? " diária" : " diárias").append('\n');
            text.append("Diárias: ").append(TextFormat.money(subtotal)).append('\n');
            if (discount.signum() > 0) {
                text.append("Desconto: ").append(TextFormat.money(discount)).append('\n');
            }
            text.append("Total: ").append(TextFormat.money(total)).append('\n');
            return text.toString();
        }
    }

    record CompanyInvoice(
            long reservationId,
            String guestName,
            String companyCnpj,
            String companyName,
            int roomNumber,
            RoomType roomType,
            LocalDate checkIn,
            LocalDate checkOut,
            int nights,
            BigDecimal subtotal,
            BigDecimal discount,
            BigDecimal total)
            implements DocumentText {

        @Override
        public String text() {
            StringBuilder text = new StringBuilder();
            text.append("Fatura de hospedagem nº ").append(reservationId).append('\n');
            text.append("Empresa: ").append(companyName).append(" (CNPJ ").append(companyCnpj).append(")\n");
            text.append("Hóspede: ").append(guestName).append('\n');
            text.append("Quarto: ").append(roomNumber).append(" (").append(roomType).append(")\n");
            text.append("Entrada: ").append(TextFormat.date(checkIn))
                    .append(" | Saída: ").append(TextFormat.date(checkOut))
                    .append(" | ").append(nights).append(nights == 1 ? " diária" : " diárias").append('\n');
            text.append("Diárias: ").append(TextFormat.money(subtotal)).append('\n');
            if (discount.signum() > 0) {
                text.append("Desconto: ").append(TextFormat.money(discount)).append('\n');
            }
            text.append("Total: ").append(TextFormat.money(total)).append('\n');
            BigDecimal iss = total.multiply(new BigDecimal("0.05")).setScale(2, RoundingMode.HALF_EVEN);
            text.append("ISS retido (5%): ").append(TextFormat.money(iss)).append('\n');
            return text.toString();
        }
    }

    record PassportBooking(
            long reservationId,
            String guestName,
            String guestPassport,
            String guestCountry,
            int roomNumber,
            RoomType roomType,
            LocalDate checkIn,
            LocalDate checkOut,
            int nights,
            BigDecimal subtotal,
            BigDecimal discount,
            BigDecimal total)
            implements DocumentText {

        @Override
        public String text() {
            StringBuilder text = new StringBuilder();
            text.append("Booking receipt no. ").append(reservationId).append('\n');
            text.append("Guest: ").append(guestName).append(" (passport ").append(guestPassport)
                    .append(", ").append(guestCountry).append(")\n");
            text.append("Room: ").append(roomNumber).append(" (").append(roomType).append(")\n");
            text.append("Check-in: ").append(checkIn)
                    .append(" | Check-out: ").append(checkOut)
                    .append(" | ").append(nights).append(nights == 1 ? " night" : " nights").append('\n');
            text.append("Nights: ").append(formatMoney(subtotal)).append('\n');
            if (discount.signum() > 0) {
                text.append("Discount: ").append(formatMoney(discount)).append('\n');
            }
            text.append("Total: ").append(formatMoney(total)).append('\n');
            return text.toString();
        }

        private String formatMoney(BigDecimal amount) {
            return "BRL " + String.format(java.util.Locale.US, "%,.2f", amount);
        }
    }

    static DocumentText create(Identification identification, long reservationId, String guestName,
            int roomNumber, RoomType roomType, LocalDate checkIn, LocalDate checkOut, int nights,
            BigDecimal subtotal, BigDecimal discount, BigDecimal total) {
        return switch (identification) {
            case Identification.CpfIdentification cpf ->
                new CpfReceipt(reservationId, guestName, cpf.cpf(), roomNumber, roomType, checkIn, checkOut,
                        nights, subtotal, discount, total);
            case Identification.CompanyIdentification company ->
                new CompanyInvoice(reservationId, guestName, company.cnpj(), company.companyName(), roomNumber,
                        roomType, checkIn, checkOut, nights, subtotal, discount, total);
            case Identification.PassportIdentification passport ->
                new PassportBooking(reservationId, guestName, passport.passport(), passport.country(), roomNumber,
                        roomType, checkIn, checkOut, nights, subtotal, discount, total);
        };
    }
}
