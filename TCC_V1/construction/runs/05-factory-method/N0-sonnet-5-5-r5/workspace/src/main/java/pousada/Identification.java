package pousada;

import java.math.BigDecimal;

/**
 * Como o hóspede se identifica. Cada forma define o documento que ele recebe;
 * um novo tipo (ex.: agência de turismo) basta implementar esta interface.
 */
public sealed interface Identification permits Identification.Cpf, Identification.Cnpj, Identification.Passport {

    String title();

    /** Texto completo do documento. */
    String render(Reservation r);

    default String cpf() { return null; }
    default String cnpj() { return null; }
    default String companyName() { return null; }
    default String passport() { return null; }
    default String country() { return null; }

    private static String stay(Reservation r) {
        return "Entrada: " + Dates.pt(r.checkIn()) + " | Saída: " + Dates.pt(r.checkOut()) + " | "
                + r.nights() + (r.nights() == 1 ? " diária" : " diárias");
    }

    private static String body(Reservation r) {
        StringBuilder sb = new StringBuilder();
        sb.append("Quarto: ").append(r.roomNumber()).append(" (").append(r.roomType()).append(")\n");
        sb.append(stay(r)).append('\n');
        sb.append("Diárias: ").append(Money.pt(r.subtotal())).append('\n');
        if (r.discount().signum() > 0) sb.append("Desconto: ").append(Money.pt(r.discount())).append('\n');
        sb.append("Total: ").append(Money.pt(r.total()));
        return sb.toString();
    }

    record Cpf(String number) implements Identification {
        public String title() { return "Comprovante de reserva"; }
        public String cpf() { return number; }
        public String render(Reservation r) {
            return title() + " nº " + r.id() + "\n"
                    + "Hóspede: " + r.guestName() + " (CPF " + number + ")\n"
                    + body(r);
        }
    }

    record Cnpj(String number, String name) implements Identification {
        public static final BigDecimal ISS_RATE = new BigDecimal("0.05");
        public String title() { return "Fatura de hospedagem"; }
        public String cnpj() { return number; }
        public String companyName() { return name; }
        public String render(Reservation r) {
            return title() + " nº " + r.id() + "\n"
                    + "Empresa: " + name + " (CNPJ " + number + ")\n"
                    + "Hóspede: " + r.guestName() + "\n"
                    + body(r) + "\n"
                    + "ISS retido (5%): " + Money.pt(Money.round(r.total().multiply(ISS_RATE)));
        }
    }

    record Passport(String number, String countryName) implements Identification {
        public String title() { return "Booking receipt"; }
        public String passport() { return number; }
        public String country() { return countryName; }
        public String render(Reservation r) {
            StringBuilder sb = new StringBuilder();
            sb.append(title()).append(" no. ").append(r.id()).append('\n');
            sb.append("Guest: ").append(r.guestName()).append(" (passport ").append(number).append(", ")
                    .append(countryName).append(")\n");
            sb.append("Room: ").append(r.roomNumber()).append(" (").append(r.roomType()).append(")\n");
            sb.append("Check-in: ").append(r.checkIn()).append(" | Check-out: ").append(r.checkOut())
                    .append(" | ").append(r.nights()).append(r.nights() == 1 ? " night" : " nights").append('\n');
            sb.append("Nights: ").append(Money.en(r.subtotal())).append('\n');
            if (r.discount().signum() > 0) sb.append("Discount: ").append(Money.en(r.discount())).append('\n');
            sb.append("Total: ").append(Money.en(r.total()));
            return sb.toString();
        }
    }
}
