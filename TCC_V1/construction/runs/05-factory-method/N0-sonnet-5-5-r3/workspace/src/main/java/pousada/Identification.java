package pousada;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Como o hóspede se identifica. Cada forma sabe emitir o próprio documento
 * (comprovante, fatura, booking receipt...). Para um novo tipo de documento
 * (ex.: agência de turismo), basta uma nova implementação e o seu mapeamento
 * em {@link #from} / na persistência.
 */
public interface Identification {

    /** Dados da reserva necessários para emitir um documento. */
    record Receipt(long reservationId, String guestName, int roomNumber, Domain.RoomType roomType,
                   LocalDate checkIn, LocalDate checkOut, int nights,
                   BigDecimal subtotal, BigDecimal discount, BigDecimal total) {}

    String title();

    String render(Receipt r);

    default String cpf() { return null; }
    default String cnpj() { return null; }
    default String companyName() { return null; }
    default String passport() { return null; }
    default String country() { return null; }

    DateTimeFormatter BR_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    static String digits(String s) {
        return s.replaceAll("\\D", "");
    }

    static String formatCpf(String s) {
        String d = digits(s);
        return d.length() == 11
                ? d.substring(0, 3) + "." + d.substring(3, 6) + "." + d.substring(6, 9) + "-" + d.substring(9)
                : s;
    }

    static String formatCnpj(String s) {
        String d = digits(s);
        return d.length() == 14
                ? d.substring(0, 2) + "." + d.substring(2, 5) + "." + d.substring(5, 8) + "/"
                        + d.substring(8, 12) + "-" + d.substring(12)
                : s;
    }

    /** Linhas comuns aos documentos em português. */
    private static String ptBody(Receipt r) {
        StringBuilder sb = new StringBuilder();
        sb.append("Quarto: ").append(r.roomNumber()).append(" (").append(r.roomType()).append(")\n");
        sb.append("Entrada: ").append(BR_DATE.format(r.checkIn()))
          .append(" | Saída: ").append(BR_DATE.format(r.checkOut()))
          .append(" | ").append(r.nights()).append(r.nights() == 1 ? " diária" : " diárias").append('\n');
        sb.append("Diárias: R$ ").append(Money.br(r.subtotal())).append('\n');
        if (r.discount().signum() > 0) {
            sb.append("Desconto: R$ ").append(Money.br(r.discount())).append('\n');
        }
        sb.append("Total: R$ ").append(Money.br(r.total()));
        return sb.toString();
    }

    /** Pessoa física, com CPF. */
    record Individual(String cpf) implements Identification {
        public String title() { return "Comprovante de reserva"; }

        public String render(Receipt r) {
            return "Comprovante de reserva nº " + r.reservationId() + "\n"
                    + "Hóspede: " + r.guestName() + " (CPF " + formatCpf(cpf) + ")\n"
                    + ptBody(r);
        }
    }

    /** Empresa, com CNPJ; recebe fatura com ISS retido. */
    record Company(String cnpj, String companyName) implements Identification {
        private static final BigDecimal ISS = new BigDecimal("0.05");

        public String title() { return "Fatura de hospedagem"; }

        public String render(Receipt r) {
            return "Fatura de hospedagem nº " + r.reservationId() + "\n"
                    + "Empresa: " + companyName + " (CNPJ " + formatCnpj(cnpj) + ")\n"
                    + "Hóspede: " + r.guestName() + "\n"
                    + ptBody(r) + "\n"
                    + "ISS retido (5%): R$ " + Money.br(Money.round(r.total().multiply(ISS)));
        }
    }

    /** Estrangeiro, com passaporte; recebe o booking receipt em inglês. */
    record Foreigner(String passport, String country) implements Identification {
        private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_LOCAL_DATE;

        public String title() { return "Booking receipt"; }

        public String render(Receipt r) {
            StringBuilder sb = new StringBuilder();
            sb.append("Booking receipt no. ").append(r.reservationId()).append('\n');
            sb.append("Guest: ").append(r.guestName()).append(" (passport ").append(passport)
              .append(", ").append(country).append(")\n");
            sb.append("Room: ").append(r.roomNumber()).append(" (").append(r.roomType()).append(")\n");
            sb.append("Check-in: ").append(ISO.format(r.checkIn()))
              .append(" | Check-out: ").append(ISO.format(r.checkOut()))
              .append(" | ").append(r.nights()).append(r.nights() == 1 ? " night" : " nights").append('\n');
            sb.append("Nights: BRL ").append(Money.en(r.subtotal())).append('\n');
            if (r.discount().signum() > 0) {
                sb.append("Discount: BRL ").append(Money.en(r.discount())).append('\n');
            }
            sb.append("Total: BRL ").append(Money.en(r.total()));
            return sb.toString();
        }
    }

    /** Monta a identificação a partir dos cinco campos (os não usados são null). */
    static Identification from(String cpf, String cnpj, String companyName, String passport, String country) {
        if (cpf != null) return new Individual(cpf);
        if (cnpj != null) return new Company(cnpj, companyName);
        return new Foreigner(passport, country);
    }
}
