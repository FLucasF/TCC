package br.com.marealta.servico;

import br.com.marealta.dominio.Dinheiro;
import br.com.marealta.dominio.Reserva;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/** Comprovante de reserva paga, e o texto dele. */
public record Comprovante(
        String title,
        int reservationId,
        String guestName,
        String guestCpf,
        int roomNumber,
        String roomType,
        LocalDate checkIn,
        LocalDate checkOut,
        int nights,
        BigDecimal subtotal,
        BigDecimal discount,
        BigDecimal total) {

    private static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public static Comprovante de(Reserva reserva) {
        return new Comprovante(
                "Comprovante de reserva",
                reserva.id(),
                reserva.hospede().nome(),
                reserva.hospede().cpf(),
                reserva.quarto().numero(),
                reserva.quarto().tipo().name(),
                reserva.entrada(),
                reserva.saida(),
                reserva.noites(),
                reserva.diarias(),
                reserva.desconto(),
                reserva.total());
    }

    public String texto() {
        StringBuilder texto = new StringBuilder();
        texto.append(title).append(" nº ").append(reservationId).append('\n');
        texto.append("Hóspede: ").append(guestName).append(" (CPF ").append(guestCpf).append(")\n");
        texto.append("Quarto: ").append(roomNumber).append(" (").append(roomType).append(")\n");
        texto.append("Entrada: ").append(DATA.format(checkIn))
                .append(" | Saída: ").append(DATA.format(checkOut))
                .append(" | ").append(nights).append(nights == 1 ? " diária" : " diárias").append('\n');
        texto.append("Diárias: ").append(Dinheiro.formatar(subtotal)).append('\n');
        if (discount.signum() > 0) {
            texto.append("Desconto: ").append(Dinheiro.formatar(discount)).append('\n');
        }
        texto.append("Total: ").append(Dinheiro.formatar(total));
        return texto.toString();
    }
}
